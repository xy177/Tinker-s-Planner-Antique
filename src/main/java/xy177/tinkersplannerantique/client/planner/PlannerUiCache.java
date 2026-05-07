package xy177.tinkersplannerantique.client.planner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.materials.MaterialTypes;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.traits.ITrait;
import xy177.tinkersplannerantique.PlannerConfig;

final class PlannerUiCache {

    private static final String FILE_NAME = "planner_ui_cache.dat";
    private static final int VERSION = 2;
    private static final Map<String, Map<String, String>> embossModifiers = new LinkedHashMap<>();
    private static final Map<String, List<String>> specialModifiers = new LinkedHashMap<>();
    private static boolean loaded;

    private PlannerUiCache() {
    }

    static Map<String, String> getEmbossModifiers(String key) {
        load();
        Map<String, String> cached = embossModifiers.get(key);
        return cached == null ? null : new LinkedHashMap<>(cached);
    }

    static void putEmbossModifiers(String key, Map<String, String> values) {
        if (key == null || key.isEmpty() || values == null) {
            return;
        }
        load();
        embossModifiers.put(key, new LinkedHashMap<>(values));
        save(false);
    }

    static List<String> getSpecialModifiers(String key) {
        load();
        List<String> cached = specialModifiers.get(key);
        return cached == null ? null : new ArrayList<>(cached);
    }

    static void putSpecialModifiers(String key, List<String> values) {
        if (key == null || key.isEmpty() || values == null) {
            return;
        }
        load();
        specialModifiers.put(key, new ArrayList<>(values));
        save(false);
    }

    static void rebuildAll(List<? extends PlannerTarget> targets) {
        load();
        embossModifiers.clear();
        specialModifiers.clear();
        if (targets == null) {
            save();
            return;
        }
        for (PlannerTarget target : targets) {
            if (target == null || target.getRequiredComponents() == null) {
                continue;
            }
            for (int partIndex = 0; partIndex < target.getRequiredComponents().size(); partIndex++) {
                String embossKey = buildEmbossCacheKey(target, partIndex, false);
                if (!embossKey.isEmpty()) {
                    embossModifiers.put(embossKey, buildEmbossModifierCache(target, partIndex, false));
                }
                String secondKey = buildEmbossCacheKey(target, partIndex, true);
                if (!secondKey.isEmpty()) {
                    embossModifiers.put(secondKey, buildEmbossModifierCache(target, partIndex, true));
                }
            }
            String specialKey = buildSpecialModifiersCacheKey(target);
            if (!specialKey.isEmpty()) {
                specialModifiers.put(specialKey, buildSpecialModifiersCache(target));
            }
        }
        save(true);
    }

    static void clearMemory() {
        embossModifiers.clear();
        loaded = false;
    }

    static boolean hasFile() {
        return getFile().exists();
    }

    static String buildEmbossCacheKey(PlannerTarget target, int partIndex, boolean second) {
        if (target == null || target.getRequiredComponents() == null || partIndex < 0 || partIndex >= target.getRequiredComponents().size()) {
            return "";
        }
        return target.getType().name()
            + "|"
            + String.valueOf(target.getItem().getRegistryName())
            + "|"
            + partIndex
            + "|"
            + getPartTypeCacheKey(target.getRequiredComponents().get(partIndex), target.getType())
            + "|"
            + (second ? "emboss2" : "emboss");
    }

    static String buildSpecialModifiersCacheKey(PlannerTarget target) {
        if (target == null || target.getItem() == null) {
            return "";
        }
        return target.getType().name()
            + "|"
            + String.valueOf(target.getItem().getRegistryName())
            + "|special";
    }

    static Map<String, String> buildEmbossModifierCache(PlannerTarget target, int partIndex, boolean second) {
        Map<String, String> cache = new LinkedHashMap<>();
        if (target == null || target.getRequiredComponents() == null || partIndex < 0 || partIndex >= target.getRequiredComponents().size()) {
            return cache;
        }
        PartMaterialType partType = target.getRequiredComponents().get(partIndex);
        for (IModifier modifier : target.getAvailableModifiers()) {
            if (modifier == null) {
                continue;
            }
            if (second ? isSecondEmbossModifier(target.getType(), modifier) : isEmbossModifier(target.getType(), modifier)) {
                Material modifierMaterial = getModifierMaterial(modifier);
                if (modifierMaterial != null && matchesEmbossPart(target, partType, modifier, partIndex)) {
                    cache.put(modifierMaterial.getIdentifier(), modifier.getIdentifier());
                }
            }
        }
        return cache;
    }

    static List<String> buildSpecialModifiersCache(PlannerTarget target) {
        List<String> cache = new ArrayList<>();
        if (target == null) {
            return cache;
        }
        for (IModifier modifier : target.getAvailableModifiers()) {
            if (modifier == null) {
                continue;
            }
            if (isEmbossModifier(target.getType(), modifier) || isSecondEmbossModifier(target.getType(), modifier) || isMaterialSpecialModifier(target.getType(), modifier)) {
                String identifier = modifier.getIdentifier();
                if (identifier != null && !identifier.isEmpty() && !cache.contains(identifier)) {
                    cache.add(identifier);
                }
            }
        }
        return cache;
    }

    static boolean matchesEmbossPart(PlannerTarget target, PartMaterialType partType, IModifier modifier, int partIndex) {
        if (target == null || partType == null || modifier == null || target.getRequiredComponents() == null || partIndex < 0 || partIndex >= target.getRequiredComponents().size()) {
            return false;
        }
        Set<String> modifierTraits = getModifierTraitIdentifiers(modifier);
        Set<String> partTraits = getPartTraitIdentifiers(partType, getModifierMaterial(modifier), target.getType());
        return !modifierTraits.isEmpty() && modifierTraits.equals(partTraits);
    }

    private static boolean isEmbossModifier(PlannerTarget.TargetType type, IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return type == PlannerTarget.TargetType.ARMOR
            ? className.contains("modextraarmortrait") && !className.contains("modextraarmortrait2") && !className.contains("display")
            : className.contains("modextratrait") && !className.contains("modextratrait2") && !className.contains("display");
    }

    private static boolean isSecondEmbossModifier(PlannerTarget.TargetType type, IModifier modifier) {
        return MoreTConCompat.isSecondEmbossModifier(type, modifier);
    }

    private static boolean isMaterialSpecialModifier(PlannerTarget.TargetType type, IModifier modifier) {
        if (modifier == null) {
            return false;
        }
        String className = modifier.getClass().getName().toLowerCase(Locale.ROOT);
        return type == PlannerTarget.TargetType.ARMOR
            ? className.contains("modpolished") && !className.contains("display")
            : className.contains("modfortify") && !className.contains("display");
    }

    private static String getPartTypeCacheKey(PartMaterialType partType, PlannerTarget.TargetType type) {
        if (partType == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String statType : getUsedStatTypes(partType, type)) {
            builder.append(statType).append(',');
        }
        return builder.toString();
    }

    static List<String> getUsedStatTypes(PartMaterialType partType, PlannerTarget.TargetType type) {
        List<String> statTypes = new ArrayList<>();
        for (String statType : getDeclaredStatTypes(partType, type)) {
            if (!statTypes.contains(statType)) {
                statTypes.add(statType);
            }
        }
        addUsedStatType(statTypes, partType, MaterialTypes.HEAD);
        addUsedStatType(statTypes, partType, MaterialTypes.HANDLE);
        addUsedStatType(statTypes, partType, MaterialTypes.EXTRA);
        addUsedStatType(statTypes, partType, MaterialTypes.BOW);
        addUsedStatType(statTypes, partType, MaterialTypes.BOWSTRING);
        addUsedStatType(statTypes, partType, MaterialTypes.SHAFT);
        addUsedStatType(statTypes, partType, MaterialTypes.FLETCHING);
        addUsedStatType(statTypes, partType, MaterialTypes.PROJECTILE);
        if (isYoyosLoaded() && type != null) {
            addUsedStatType(statTypes, partType, "body");
            addUsedStatType(statTypes, partType, "cord");
            addUsedStatType(statTypes, partType, "axle");
        }
        if (type == PlannerTarget.TargetType.ARMOR && ConArmPresence.isLoaded()) {
            addUsedStatType(statTypes, partType, "core");
            addUsedStatType(statTypes, partType, "plates");
            addUsedStatType(statTypes, partType, "trim");
        }
        return statTypes;
    }

    private static List<String> getDeclaredStatTypes(PartMaterialType partType, PlannerTarget.TargetType type) {
        List<String> result = new ArrayList<>();
        if (partType == null) {
            return result;
        }
        Class<?> partTypeClass = partType.getClass();
        while (partTypeClass != null) {
            try {
                Field field = partTypeClass.getDeclaredField("neededTypes");
                field.setAccessible(true);
                Object value = field.get(partType);
                if (value instanceof String[]) {
                    for (String statType : (String[]) value) {
                        if (!result.contains(statType)) {
                            result.add(statType);
                        }
                    }
                }
                break;
            } catch (NoSuchFieldException ignored) {
                partTypeClass = partTypeClass.getSuperclass();
            } catch (IllegalAccessException ignored) {
                break;
            }
        }
        if (isYoyosLoaded()) {
            if (partType.usesStat("body")) {
                result.add("body");
            }
            if (partType.usesStat("cord")) {
                result.add("cord");
            }
            if (partType.usesStat("axle")) {
                result.add("axle");
            }
        }
        if (type == PlannerTarget.TargetType.ARMOR && ConArmPresence.isLoaded()) {
            if (partType.usesStat("core")) {
                result.add("core");
            }
            if (partType.usesStat("plates")) {
                result.add("plates");
            }
            if (partType.usesStat("trim")) {
                result.add("trim");
            }
        }
        return result;
    }

    private static void addUsedStatType(List<String> statTypes, PartMaterialType partType, String statType) {
        if (partType.usesStat(statType) && !statTypes.contains(statType)) {
            statTypes.add(statType);
        }
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

    static Set<String> getPartTraitIdentifiers(PartMaterialType partType, Material material, PlannerTarget.TargetType type) {
        Set<String> result = new LinkedHashSet<>();
        if (partType == null || material == null) {
            return result;
        }
        for (String statType : getUsedStatTypes(partType, type)) {
            for (ITrait trait : material.getAllTraitsForStats(statType)) {
                result.add(trait.getIdentifier());
            }
        }
        return result;
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

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        File file = getFile();
        if (!file.exists()) {
            return;
        }
        try {
            NBTTagCompound root = CompressedStreamTools.read(file);
            if (root == null || root.getInteger("version") != VERSION || !getFingerprint().equals(root.getString("fingerprint"))) {
                return;
            }
            NBTTagList entries = root.getTagList("embossModifiers", 10);
            for (int i = 0; i < entries.tagCount(); i++) {
                NBTTagCompound entry = entries.getCompoundTagAt(i);
                String key = entry.getString("key");
                if (key.isEmpty()) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                NBTTagList mappings = entry.getTagList("values", 10);
                for (int j = 0; j < mappings.tagCount(); j++) {
                    NBTTagCompound mapping = mappings.getCompoundTagAt(j);
                    String material = mapping.getString("material");
                    String modifier = mapping.getString("modifier");
                    if (!material.isEmpty() && !modifier.isEmpty()) {
                        values.put(material, modifier);
                    }
                }
                embossModifiers.put(key, values);
            }
            NBTTagList specialEntries = root.getTagList("specialModifiers", 10);
            for (int i = 0; i < specialEntries.tagCount(); i++) {
                NBTTagCompound entry = specialEntries.getCompoundTagAt(i);
                String key = entry.getString("key");
                if (key.isEmpty()) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                NBTTagList identifiers = entry.getTagList("values", 8);
                for (int j = 0; j < identifiers.tagCount(); j++) {
                    String identifier = identifiers.getStringTagAt(j);
                    if (!identifier.isEmpty()) {
                        values.add(identifier);
                    }
                }
                specialModifiers.put(key, values);
            }
        } catch (IOException ignored) {
        }
    }

    private static void save() {
        save(false);
    }

    private static void save(boolean force) {
        try {
            File file = getFile();
            if (!force && !PlannerConfig.enablePlannerUiCache && file.exists()) {
                return;
            }
            File folder = file.getParentFile();
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }
            NBTTagCompound root = new NBTTagCompound();
            root.setInteger("version", VERSION);
            root.setString("fingerprint", getFingerprint());
            NBTTagList entries = new NBTTagList();
            for (Map.Entry<String, Map<String, String>> entry : embossModifiers.entrySet()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("key", entry.getKey());
                NBTTagList mappings = new NBTTagList();
                for (Map.Entry<String, String> mapping : entry.getValue().entrySet()) {
                    NBTTagCompound value = new NBTTagCompound();
                    value.setString("material", mapping.getKey());
                    value.setString("modifier", mapping.getValue());
                    mappings.appendTag(value);
                }
                tag.setTag("values", mappings);
                entries.appendTag(tag);
            }
            root.setTag("embossModifiers", entries);
            NBTTagList specialEntries = new NBTTagList();
            for (Map.Entry<String, List<String>> entry : specialModifiers.entrySet()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("key", entry.getKey());
                NBTTagList values = new NBTTagList();
                for (String identifier : entry.getValue()) {
                    values.appendTag(new NBTTagString(identifier));
                }
                tag.setTag("values", values);
                specialEntries.appendTag(tag);
            }
            root.setTag("specialModifiers", specialEntries);
            CompressedStreamTools.write(root, file);
        } catch (IOException ignored) {
        }
    }

    private static String getFingerprint() {
        StringBuilder builder = new StringBuilder(4096);
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            builder.append(mod.getModId()).append('@').append(mod.getVersion()).append(';');
        }
        builder.append("|materials=");
        for (Material material : TinkerRegistry.getAllMaterials()) {
            if (material != null && material != Material.UNKNOWN && material.getIdentifier() != null) {
                builder.append(material.getIdentifier()).append(',');
            }
        }
        builder.append("|modifiers=");
        for (IModifier modifier : TinkerRegistry.getAllModifiers()) {
            if (modifier != null && modifier.getIdentifier() != null) {
                builder.append(modifier.getIdentifier()).append('@').append(modifier.getClass().getName()).append(',');
            }
        }
        builder.append("|conarm=").append(ConArmPresence.isLoaded());
        if (ConArmPresence.isLoaded()) {
            for (IModifier modifier : ConArmCompat.getAllArmorModifiers()) {
                if (modifier != null && modifier.getIdentifier() != null) {
                    builder.append(modifier.getIdentifier()).append('@').append(modifier.getClass().getName()).append(',');
                }
            }
        }
        return Integer.toHexString(builder.toString().hashCode());
    }

    private static File getFile() {
        return new File(PlannerClientEvents.getDataFolder(), FILE_NAME);
    }

    private static boolean isYoyosLoaded() {
        return Loader.isModLoaded("yoyos");
    }
}
