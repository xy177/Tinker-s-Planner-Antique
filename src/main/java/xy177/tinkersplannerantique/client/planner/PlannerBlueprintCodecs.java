package xy177.tinkersplannerantique.client.planner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.materials.MaterialTypes;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.traits.ITrait;

final class PlannerBlueprintCodecs {

    static final String STANDARD_PREFIX = "TPA1:";

    private PlannerBlueprintCodecs() {
    }

    static String exportStandard(PlannerBlueprint blueprint) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(blueprint.toTag(), output);
        return STANDARD_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
    }

    static String exportShort(PlannerBlueprint blueprint, PlannerShortCodeList list) {
        StringBuilder code = new StringBuilder();
        code.append('[').append(escapeHeader(list.validationName)).append(']').append('~');
        code.append('[').append(escapeHeader(blueprint.target.getDisplayName())).append(']').append('~');
        code.append(list.getTargetCode(blueprint.target)).append(Integer.toString(Math.max(0, blueprint.toolLevel), 36)).append('~');
        code.append(exportMaterials(blueprint, list)).append('~');
        code.append(exportModifiers(blueprint, list)).append('~');
        code.append(exportMaterialSpecial(blueprint, list)).append('~');
        code.append(exportEmboss(blueprint, list, false)).append('~');
        code.append(exportEmboss(blueprint, list, true));
        return code.toString();
    }

    static PlannerBlueprint importCode(String input, PlannerShortCodeList shortList, Collection<? extends PlannerTarget> allTargets) throws IOException {
        String trimmed = input == null ? "" : input.trim();
        int standardIndex = trimmed.indexOf(STANDARD_PREFIX);
        if (standardIndex >= 0) {
            return importStandard(trimmed.substring(standardIndex), allTargets);
        }
        return importShort(trimmed, shortList, allTargets);
    }

    private static String exportMaterials(PlannerBlueprint blueprint, PlannerShortCodeList list) {
        StringBuilder builder = new StringBuilder();
        for (Material material : blueprint.materials) {
            builder.append(list.getMaterialCode(material));
        }
        return builder.toString();
    }

    private static String exportModifiers(PlannerBlueprint blueprint, PlannerShortCodeList list) {
        List<String> ids = new ArrayList<>();
        for (String identifier : new LinkedHashSet<>(blueprint.modifiers)) {
            ids.add(identifier);
        }
        ids.sort((left, right) -> list.getModifierCode(blueprint.target.resolveModifier(left)).compareTo(list.getModifierCode(blueprint.target.resolveModifier(right))));
        StringBuilder builder = new StringBuilder();
        for (String identifier : ids) {
            IModifier modifier = blueprint.target.resolveModifier(identifier);
            int level = blueprint.getModifierLevel(identifier);
            if (modifier == null || level <= 0) {
                continue;
            }
            builder.append(list.getModifierCode(modifier));
            builder.append(encodeLevel(level));
        }
        return builder.toString();
    }

    private static String exportMaterialSpecial(PlannerBlueprint blueprint, PlannerShortCodeList list) {
        if (!blueprint.hasMaterialModifier()) {
            return PlannerShortCodeList.PLACEHOLDER;
        }
        IModifier modifier = blueprint.target.resolveModifier(blueprint.materialModifierId);
        return list.getMaterialCode(getModifierMaterial(modifier));
    }

    private static String exportEmboss(PlannerBlueprint blueprint, PlannerShortCodeList list, boolean second) {
        StringBuilder builder = new StringBuilder();
        String materialCode = PlannerShortCodeList.PLACEHOLDER;
        boolean hasEmboss = second ? blueprint.hasSecondEmboss() : blueprint.hasEmboss();
        int partIndex = second ? blueprint.secondEmbossPartIndex : blueprint.embossPartIndex;
        String modifierId = second ? blueprint.secondEmbossModifierId : blueprint.embossModifierId;
        if (hasEmboss) {
            IModifier modifier = blueprint.target.resolveModifier(modifierId);
            materialCode = list.getMaterialCode(getModifierMaterial(modifier));
        }
        for (int i = 0; i < blueprint.materials.length; i++) {
            builder.append(hasEmboss && partIndex == i ? materialCode : PlannerShortCodeList.PLACEHOLDER);
        }
        return builder.toString();
    }

    private static PlannerBlueprint importStandard(String code, Collection<? extends PlannerTarget> allTargets) throws IOException {
        String payload = code.substring(STANDARD_PREFIX.length()).trim();
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new IOException("gui.tpa.error.invalid_standard_code");
        }
        NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
        PlannerBlueprint blueprint = PlannerBlueprint.fromTag(tag, new ArrayList<>(allTargets));
        if (blueprint == null) {
            throw new IOException("gui.tpa.error.invalid_standard_code");
        }
        return blueprint;
    }

    private static PlannerBlueprint importShort(String code, PlannerShortCodeList shortList, Collection<? extends PlannerTarget> allTargets) throws IOException {
        if (shortList == null) {
            throw new IOException("gui.tpa.short_list_missing");
        }
        HeaderData headers = stripHeaders(code);
        if (headers.validationName != null && !headers.validationName.equals(shortList.validationName)) {
            throw new IOException("gui.tpa.error.validation_name_mismatch");
        }
        String[] parts = headers.body.split("~", -1);
        if (parts.length < 5) {
            throw new IOException("gui.tpa.error.invalid_short_code");
        }
        String targetPart = parts[0];
        if (targetPart.length() < 2) {
            throw new IOException("gui.tpa.error.invalid_short_code");
        }
        PlannerTarget target = shortList.findTarget(targetPart.substring(0, 2), allTargets);
        if (target == null) {
            throw new IOException("gui.tpa.error.unknown_short_target");
        }
        PlannerBlueprint blueprint = new PlannerBlueprint(target);
        blueprint.toolLevel = decodeBase36Int(targetPart.length() > 2 ? targetPart.substring(2) : "0");
        importMaterials(blueprint, parts[1], shortList);
        importModifiers(blueprint, parts[2], shortList, allTargets);
        importMaterialSpecial(blueprint, parts[3], shortList);
        importEmboss(blueprint, parts[4], shortList);
        if (parts.length > 5 && blueprint.hasEmboss()) {
            importSecondEmboss(blueprint, parts[5], shortList);
        }
        return blueprint;
    }

    private static void importMaterials(PlannerBlueprint blueprint, String materialsCode, PlannerShortCodeList list) throws IOException {
        for (int i = 0; i < blueprint.materials.length; i++) {
            int start = i * 2;
            if (materialsCode.length() < start + 2) {
                break;
            }
            String code = materialsCode.substring(start, start + 2);
            if (list.isMissingMaterialCode(code)) {
                throw new IOException("gui.tpa.error.missing_short_material");
            }
            Material material = list.findMaterial(code);
            if (material != null) {
                blueprint.materials[i] = material;
            }
        }
    }

    private static void importModifiers(PlannerBlueprint blueprint, String modifiersCode, PlannerShortCodeList list, Collection<? extends PlannerTarget> allTargets) {
        for (int i = 0; i + 2 < modifiersCode.length(); i += 3) {
            IModifier modifier = list.findModifier(modifiersCode.substring(i, i + 2), allTargets);
            int level = decodeLevel(modifiersCode.charAt(i + 2));
            if (modifier == null || level <= 0) {
                continue;
            }
            int currentLevel = blueprint.getModifierLevel(modifier.getIdentifier());
            while (currentLevel < level) {
                if (!blueprint.addModifier(modifier)) {
                    break;
                }
                int nextLevel = blueprint.getModifierLevel(modifier.getIdentifier());
                if (nextLevel <= currentLevel) {
                    break;
                }
                currentLevel = nextLevel;
            }
        }
    }

    private static void importMaterialSpecial(PlannerBlueprint blueprint, String code, PlannerShortCodeList list) throws IOException {
        if (list.isMissingMaterialCode(code)) {
            throw new IOException("gui.tpa.error.missing_short_material");
        }
        Material material = list.findMaterial(code);
        if (material == null) {
            return;
        }
        IModifier modifier = findMaterialSpecialModifier(blueprint.target, material);
        if (modifier != null) {
            blueprint.setMaterialModifier(modifier.getIdentifier());
        }
    }

    private static void importEmboss(PlannerBlueprint blueprint, String code, PlannerShortCodeList list) throws IOException {
        importEmboss(blueprint, code, list, false);
    }

    private static void importSecondEmboss(PlannerBlueprint blueprint, String code, PlannerShortCodeList list) throws IOException {
        importEmboss(blueprint, code, list, true);
    }

    private static void importEmboss(PlannerBlueprint blueprint, String code, PlannerShortCodeList list, boolean second) throws IOException {
        for (int i = 0; i < blueprint.materials.length; i++) {
            int start = i * 2;
            if (code.length() < start + 2) {
                break;
            }
            String materialCode = code.substring(start, start + 2);
            if (list.isMissingMaterialCode(materialCode)) {
                throw new IOException("gui.tpa.error.missing_short_material");
            }
            Material material = list.findMaterial(materialCode);
            if (material == null) {
                continue;
            }
            IModifier modifier = findEmbossModifier(blueprint.target, blueprint.target.getRequiredComponents().get(i), material, second);
            if (modifier != null) {
                if (second) {
                    blueprint.setSecondEmboss(i, modifier.getIdentifier());
                } else {
                    blueprint.setEmboss(i, modifier.getIdentifier());
                }
                return;
            }
        }
    }

    private static IModifier findEmbossModifier(PlannerTarget target, PartMaterialType partType, Material material, boolean second) {
        for (IModifier modifier : target.getAvailableModifiers()) {
            if ((second ? isSecondEmbossModifier(target.getType(), modifier) : isEmbossModifier(target.getType(), modifier))) {
                Material modifierMaterial = getModifierMaterial(modifier);
                if (modifierMaterial != null && modifierMaterial.getIdentifier().equals(material.getIdentifier())) {
                    Set<String> modifierTraits = getModifierTraitIdentifiers(modifier);
                    Set<String> partTraits = getPartTraitIdentifiers(partType, material);
                    if (!modifierTraits.isEmpty() && modifierTraits.equals(partTraits)) {
                        return modifier;
                    }
                }
            }
        }
        return null;
    }

    private static IModifier findMaterialSpecialModifier(PlannerTarget target, Material material) {
        for (IModifier modifier : target.getAvailableModifiers()) {
            if (isMaterialSpecialModifier(target.getType(), modifier)) {
                Material modifierMaterial = getModifierMaterial(modifier);
                if (modifierMaterial != null && modifierMaterial.getIdentifier().equals(material.getIdentifier())) {
                    return modifier;
                }
            }
        }
        return null;
    }

    private static boolean isEmbossModifier(PlannerTarget.TargetType type, IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase(Locale.ROOT);
        return type == PlannerTarget.TargetType.ARMOR ? className.contains("modextraarmortrait") && !className.contains("modextraarmortrait2") && !className.contains("display") : className.contains("modextratrait") && !className.contains("modextratrait2") && !className.contains("display");
    }

    private static boolean isSecondEmbossModifier(PlannerTarget.TargetType type, IModifier modifier) {
        return MoreTConCompat.isSecondEmbossModifier(type, modifier);
    }

    private static boolean isMaterialSpecialModifier(PlannerTarget.TargetType type, IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase(Locale.ROOT);
        return type == PlannerTarget.TargetType.ARMOR ? className.contains("modpolished") && !className.contains("display") : className.contains("modfortify") && !className.contains("display");
    }

    private static Material getModifierMaterial(IModifier modifier) {
        if (modifier == null) {
            return null;
        }
        Class<?> type = modifier.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (Material.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (Material) field.get(modifier);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Set<String> getModifierTraitIdentifiers(IModifier modifier) {
        Set<String> result = new LinkedHashSet<>();
        Class<?> type = modifier.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(modifier);
                    if (!(value instanceof Collection)) {
                        continue;
                    }
                    Collection<?> collection = (Collection<?>) value;
                    if (collection.isEmpty()) {
                        continue;
                    }
                    Object first = collection.iterator().next();
                    if (!(first instanceof ITrait)) {
                        continue;
                    }
                    for (Object entry : collection) {
                        result.add(((ITrait) entry).getIdentifier());
                    }
                    return result;
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return result;
    }

    private static Set<String> getPartTraitIdentifiers(PartMaterialType partType, Material material) {
        Set<String> result = new LinkedHashSet<>();
        for (String statType : getUsedStatTypes(partType)) {
            for (ITrait trait : material.getAllTraitsForStats(statType)) {
                result.add(trait.getIdentifier());
            }
        }
        return result;
    }

    private static List<String> getUsedStatTypes(PartMaterialType partType) {
        List<String> statTypes = new ArrayList<>();
        addUsedStatType(statTypes, partType, MaterialTypes.HEAD);
        addUsedStatType(statTypes, partType, MaterialTypes.HANDLE);
        addUsedStatType(statTypes, partType, MaterialTypes.EXTRA);
        addUsedStatType(statTypes, partType, MaterialTypes.BOW);
        addUsedStatType(statTypes, partType, MaterialTypes.BOWSTRING);
        addUsedStatType(statTypes, partType, MaterialTypes.SHAFT);
        addUsedStatType(statTypes, partType, MaterialTypes.FLETCHING);
        addUsedStatType(statTypes, partType, MaterialTypes.PROJECTILE);
        if (ConArmPresence.isLoaded()) {
            addUsedStatType(statTypes, partType, ConArmCompat.CORE);
            addUsedStatType(statTypes, partType, ConArmCompat.PLATES);
            addUsedStatType(statTypes, partType, ConArmCompat.TRIM);
        }
        return statTypes;
    }

    private static void addUsedStatType(List<String> statTypes, PartMaterialType partType, String statType) {
        if (partType.usesStat(statType)) {
            statTypes.add(statType);
        }
    }

    private static char encodeLevel(int level) {
        if (level <= 0 || level >= 36) {
            throw new IllegalArgumentException("Modifier level out of short-code range: " + level);
        }
        return "0123456789abcdefghijklmnopqrstuvwxyz".charAt(level);
    }

    private static int decodeLevel(char value) {
        int decoded = PlannerShortCodeList.decodeIndex("0" + value);
        return decoded < 0 ? 0 : decoded;
    }

    private static int decodeBase36Int(String value) throws IOException {
        try {
            return Integer.parseInt(value, 36);
        } catch (NumberFormatException e) {
            throw new IOException("gui.tpa.error.invalid_short_code");
        }
    }

    private static HeaderData stripHeaders(String code) {
        String remaining = code.trim();
        String validation = null;
        String displayName = null;
        int[] nextIndex = new int[] { 0 };
        String first = tryReadHeader(remaining, nextIndex);
        if (first != null) {
            validation = unescapeHeader(first);
            remaining = remaining.substring(nextIndex[0]);
            String second = tryReadHeader(remaining, nextIndex);
            if (second != null) {
                displayName = unescapeHeader(second);
                remaining = remaining.substring(nextIndex[0]);
            }
        }
        return new HeaderData(validation, displayName, remaining);
    }

    private static String tryReadHeader(String input, int[] nextIndex) {
        if (!input.startsWith("[")) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaping) {
                builder.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == ']') {
                if (i + 1 < input.length() && input.charAt(i + 1) == '~') {
                    nextIndex[0] = i + 2;
                    return builder.toString();
                }
                nextIndex[0] = i + 1;
                return builder.toString();
            }
            builder.append(c);
        }
        return null;
    }

    private static String escapeHeader(String value) {
        return value.replace("\\", "\\\\").replace("]", "\\]");
    }

    private static String unescapeHeader(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                builder.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    static final class HeaderData {
        final String validationName;
        final String displayName;
        final String body;

        private HeaderData(String validationName, String displayName, String body) {
            this.validationName = validationName;
            this.displayName = displayName;
            this.body = body;
        }
    }
}
