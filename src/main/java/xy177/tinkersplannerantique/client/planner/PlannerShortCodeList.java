package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import xy177.tinkersplannerantique.PlannerConfig;

final class PlannerShortCodeList {

    static final String FILE_NAME = "planner_short_list.dat";
    static final String DEFAULT_NAME = "ticlist";
    static final String PLACEHOLDER = "--";
    private static final String MISSING_PREFIX = "!missing:";
    private static final char[] BASE36 = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int MAX_CODES = BASE36.length * BASE36.length;

    final String validationName;
    final String fingerprint;
    final long generatedAt;
    private final List<String> targetIds;
    private final List<String> materialIds;
    private final List<String> modifierIds;
    private final Map<String, Integer> targetIndex = new LinkedHashMap<>();
    private final Map<String, Integer> materialIndex = new LinkedHashMap<>();
    private final Map<String, Integer> modifierIndex = new LinkedHashMap<>();

    static final class Counts {
        final int targets;
        final int materials;
        final int modifiers;

        private Counts(int targets, int materials, int modifiers) {
            this.targets = targets;
            this.materials = materials;
            this.modifiers = modifiers;
        }
    }

    private PlannerShortCodeList(String validationName, long generatedAt, List<String> targetIds, List<String> materialIds, List<String> modifierIds) {
        this.validationName = validationName;
        this.generatedAt = generatedAt;
        this.targetIds = targetIds;
        this.materialIds = materialIds;
        this.modifierIds = modifierIds;
        index(targetIds, targetIndex);
        index(materialIds, materialIndex);
        index(modifierIds, modifierIndex);
        this.fingerprint = buildFingerprint(validationName, targetIds, materialIds, modifierIds);
    }

    static PlannerShortCodeList generate(String validationName, Collection<? extends PlannerTarget> targets) {
        List<String> targetIds = collectTargetIds(targets);
        List<String> materialIds = collectMaterialIds();
        List<String> modifierIds = collectModifierIds(targets);

        ensureCapacity(targetIds, "targets");
        ensureCapacity(materialIds, "materials");
        ensureCapacity(modifierIds, "modifiers");
        return new PlannerShortCodeList(validationName == null || validationName.isEmpty() ? DEFAULT_NAME : validationName, System.currentTimeMillis(), targetIds, materialIds, modifierIds);
    }

    static Counts count(Collection<? extends PlannerTarget> targets) {
        return new Counts(collectTargetIds(targets).size(), collectMaterialIds().size(), collectModifierIds(targets).size());
    }

    private static List<String> collectTargetIds(Collection<? extends PlannerTarget> targets) {
        List<String> targetIds = new ArrayList<>();
        for (PlannerTarget target : targets) {
            if (target.getItem().getRegistryName() != null) {
                targetIds.add(target.getType().name() + "|" + target.getItem().getRegistryName());
            }
        }
        targetIds.sort(String::compareTo);
        return targetIds;
    }

    private static List<String> collectMaterialIds() {
        List<String> materialIds = new ArrayList<>();
        for (Material material : TinkerRegistry.getAllMaterials()) {
            if (material != null && material != Material.UNKNOWN && material.getIdentifier() != null && !material.getIdentifier().isEmpty()) {
                materialIds.add(material.getIdentifier());
            }
        }
        materialIds.sort(String::compareTo);
        return materialIds;
    }

    private static List<String> collectModifierIds(Collection<? extends PlannerTarget> targets) {
        Map<String, IModifier> modifiers = new LinkedHashMap<>();
        for (PlannerTarget target : targets) {
            for (IModifier modifier : target.getAvailableModifiers()) {
                if (modifier != null && !modifier.isHidden() && !isSpecialModifier(modifier)) {
                    modifiers.putIfAbsent(modifier.getIdentifier(), modifier);
                }
            }
        }
        List<String> modifierIds = new ArrayList<>(modifiers.keySet());
        modifierIds.sort(String::compareTo);
        return modifierIds;
    }

    static PlannerShortCodeList load(File rootFolder) {
        File file = new File(rootFolder, FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null) {
                return null;
            }
            return new PlannerShortCodeList(
                root.getString("validationName"),
                root.getLong("generatedAt"),
                readStringList(root.getTagList("targets", 8)),
                readStringList(root.getTagList("materials", 8)),
                readStringList(root.getTagList("modifiers", 8))
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    static PlannerShortCodeList loadOrCreate(File rootFolder, Collection<? extends PlannerTarget> targets) {
        PlannerShortCodeList list = load(rootFolder);
        if (list != null || !PlannerConfig.enableShortBlueprintCode || !PlannerConfig.autoGenerateShortBlueprintList) {
            return list;
        }
        try {
            PlannerShortCodeList generated = generate(PlannerConfig.defaultShortBlueprintListName, targets);
            generated.save(rootFolder);
            return generated;
        } catch (Exception ignored) {
            return null;
        }
    }

    void save(File rootFolder) throws IOException {
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }
        File file = new File(rootFolder, FILE_NAME);
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("version", 1);
        root.setString("validationName", validationName);
        root.setString("fingerprint", fingerprint);
        root.setLong("generatedAt", generatedAt);
        root.setTag("targets", writeStringList(targetIds));
        root.setTag("materials", writeStringList(materialIds));
        root.setTag("modifiers", writeStringList(modifierIds));
        CompressedStreamTools.write(root, file);
    }

    PlannerShortCodeList withValidationName(String newName) {
        return new PlannerShortCodeList(
            newName == null || newName.isEmpty() ? DEFAULT_NAME : newName,
            System.currentTimeMillis(),
            new ArrayList<>(targetIds),
            new ArrayList<>(materialIds),
            new ArrayList<>(modifierIds)
        );
    }

    PlannerShortCodeList remake(String newName, Collection<? extends PlannerTarget> targets) {
        List<String> updatedTargets = mergePreservingIndexes(targetIds, collectTargetIds(targets));
        List<String> updatedMaterials = mergePreservingIndexes(materialIds, collectMaterialIds());
        List<String> updatedModifiers = mergePreservingIndexes(modifierIds, collectModifierIds(targets));
        ensureCapacity(updatedTargets, "targets");
        ensureCapacity(updatedMaterials, "materials");
        ensureCapacity(updatedModifiers, "modifiers");
        String validation = newName == null || newName.isEmpty() ? validationName : newName;
        return new PlannerShortCodeList(validation == null || validation.isEmpty() ? DEFAULT_NAME : validation, System.currentTimeMillis(), updatedTargets, updatedMaterials, updatedModifiers);
    }

    Counts countEntries() {
        return new Counts(targetIds.size(), materialIds.size(), modifierIds.size());
    }

    String getTargetCode(PlannerTarget target) {
        Integer index = targetIndex.get(target.getType().name() + "|" + target.getItem().getRegistryName());
        return index == null ? PLACEHOLDER : encodeIndex(index);
    }

    String getMaterialCode(Material material) {
        if (material == null) {
            return PLACEHOLDER;
        }
        Integer index = materialIndex.get(material.getIdentifier());
        return index == null ? PLACEHOLDER : encodeIndex(index);
    }

    String getModifierCode(IModifier modifier) {
        if (modifier == null) {
            return PLACEHOLDER;
        }
        Integer index = modifierIndex.get(modifier.getIdentifier());
        return index == null ? PLACEHOLDER : encodeIndex(index);
    }

    PlannerTarget findTarget(String code, Collection<? extends PlannerTarget> targets) {
        int index = decodeIndex(code);
        if (index < 0 || index >= targetIds.size()) {
            return null;
        }
        String key = targetIds.get(index);
        for (PlannerTarget target : targets) {
            if ((target.getType().name() + "|" + target.getItem().getRegistryName()).equals(key)) {
                return target;
            }
        }
        return null;
    }

    Material findMaterial(String code) {
        int index = decodeIndex(code);
        if (index < 0 || index >= materialIds.size()) {
            return null;
        }
        String id = materialIds.get(index);
        if (isMissingEntry(id)) {
            return null;
        }
        Material material = TinkerRegistry.getMaterial(id);
        return material == null || material == Material.UNKNOWN ? null : material;
    }

    boolean isMissingMaterialCode(String code) {
        int index = decodeIndex(code);
        if (index < 0 || index >= materialIds.size()) {
            return false;
        }
        String id = materialIds.get(index);
        return isMissingEntry(id) || TinkerRegistry.getMaterial(id) == Material.UNKNOWN;
    }

    IModifier findModifier(String code, Collection<? extends PlannerTarget> targets) {
        int index = decodeIndex(code);
        if (index < 0 || index >= modifierIds.size()) {
            return null;
        }
        String id = modifierIds.get(index);
        for (PlannerTarget target : targets) {
            IModifier modifier = target.resolveModifier(id);
            if (modifier != null) {
                return modifier;
            }
        }
        return null;
    }

    static String encodeIndex(int index) {
        if (index < 0 || index >= MAX_CODES) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }
        return new String(new char[] { BASE36[index / BASE36.length], BASE36[index % BASE36.length] });
    }

    static int decodeIndex(String value) {
        if (value == null || value.length() != 2 || PLACEHOLDER.equals(value)) {
            return -1;
        }
        int high = decodeBase36(value.charAt(0));
        int low = decodeBase36(value.charAt(1));
        return high < 0 || low < 0 ? -1 : high * BASE36.length + low;
    }

    private static int decodeBase36(char c) {
        char lower = Character.toLowerCase(c);
        if (lower >= '0' && lower <= '9') {
            return lower - '0';
        }
        if (lower >= 'a' && lower <= 'z') {
            return lower - 'a' + 10;
        }
        return -1;
    }

    private static void index(List<String> values, Map<String, Integer> out) {
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (!isMissingEntry(value)) {
                out.put(value, i);
            }
        }
    }

    private static List<String> mergePreservingIndexes(List<String> existing, List<String> current) {
        Set<String> remaining = new LinkedHashSet<>(current);
        List<String> merged = new ArrayList<>(existing.size() + remaining.size());
        for (String entry : existing) {
            String id = unwrapMissingEntry(entry);
            if (remaining.remove(id)) {
                merged.add(id);
            } else {
                merged.add(markMissing(id));
            }
        }
        merged.addAll(remaining);
        return merged;
    }

    private static String markMissing(String id) {
        return isMissingEntry(id) ? id : MISSING_PREFIX + id;
    }

    private static String unwrapMissingEntry(String id) {
        return isMissingEntry(id) ? id.substring(MISSING_PREFIX.length()) : id;
    }

    private static boolean isMissingEntry(String id) {
        return id != null && id.startsWith(MISSING_PREFIX);
    }

    private static NBTTagList writeStringList(List<String> values) {
        NBTTagList list = new NBTTagList();
        for (String value : values) {
            list.appendTag(new NBTTagString(value));
        }
        return list;
    }

    private static List<String> readStringList(NBTTagList list) {
        List<String> result = new ArrayList<>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            result.add(list.getStringTagAt(i));
        }
        return result;
    }

    private static boolean isSpecialModifier(IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase(Locale.ROOT);
        return (className.contains("modextratrait") || className.contains("modextraarmortrait") || className.contains("modfortify") || className.contains("modpolished")) && !className.contains("display");
    }

    private static void ensureCapacity(List<String> values, String label) {
        if (values.size() > MAX_CODES) {
            throw new IllegalStateException("Too many " + label + " for short blueprint codes: " + values.size());
        }
    }

    private static String buildFingerprint(String validationName, List<String> targetIds, List<String> materialIds, List<String> modifierIds) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            updateDigest(digest, validationName);
            for (String entry : targetIds) {
                updateDigest(digest, entry);
            }
            for (String entry : materialIds) {
                updateDigest(digest, entry);
            }
            for (String entry : modifierIds) {
                updateDigest(digest, entry);
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4 && i < hash.length; i++) {
                builder.append(String.format("%02x", hash[i] & 0xFF));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return "00000000";
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
