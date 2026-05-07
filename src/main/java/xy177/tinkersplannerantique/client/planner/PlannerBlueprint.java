package xy177.tinkersplannerantique.client.planner;

import java.util.ArrayList;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.modifiers.ModifierNBT;
import slimeknights.tconstruct.library.modifiers.TinkerGuiException;
import slimeknights.tconstruct.library.modifiers.ModifierAspect;
import slimeknights.tconstruct.library.tinkering.TinkersItem;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.library.utils.TinkerUtil;
import slimeknights.tconstruct.library.utils.ToolBuilder;

final class PlannerBlueprint {

    final PlannerTarget target;
    final Material[] materials;
    final List<String> modifiers = new ArrayList<>();
    int toolLevel = ToolLevelingCompat.getDefaultDisplayLevel();
    int embossPartIndex = -1;
    String embossModifierId;
    int secondEmbossPartIndex = -1;
    String secondEmbossModifierId;
    String materialModifierId;
    private static final int MAX_BATCH_MODIFIER_APPLIES = 512;
    private static final MultiAspectInfo NO_MULTI_ASPECT_INFO = new MultiAspectInfo(0, 0);
    private static final Map<Class<?>, MultiAspectInfo> MULTI_ASPECT_INFO_CACHE = new HashMap<>();
    private String cachedStateKey;
    private ItemStack cachedPreview = ItemStack.EMPTY;
    private boolean hasCachedPreview;
    private final Map<String, Integer> cachedModifierLevels = new HashMap<>();
    private final Map<String, Boolean> cachedCanAddModifiers = new HashMap<>();

    PlannerBlueprint(PlannerTarget target) {
        this.target = target;
        this.materials = new Material[target.getRequiredComponents().size()];
    }

    PlannerBlueprint copy() {
        PlannerBlueprint copy = new PlannerBlueprint(target);
        System.arraycopy(materials, 0, copy.materials, 0, materials.length);
        copy.modifiers.addAll(modifiers);
        copy.toolLevel = toolLevel;
        copy.embossPartIndex = embossPartIndex;
        copy.embossModifierId = embossModifierId;
        copy.secondEmbossPartIndex = secondEmbossPartIndex;
        copy.secondEmbossModifierId = secondEmbossModifierId;
        copy.materialModifierId = materialModifierId;
        return copy;
    }

    boolean isComplete() {
        for (Material material : materials) {
            if (material == null) {
                return false;
            }
        }
        return true;
    }

    ItemStack buildPreview() {
        String stateKey = buildStateKey();
        ItemStack preview = getPreviewForState(stateKey);
        return preview.isEmpty() ? ItemStack.EMPTY : preview.copy();
    }

    private ItemStack getPreviewForState(String stateKey) {
        refreshComputedState(stateKey);
        if (!hasCachedPreview) {
            cachedPreview = createPreview();
            hasCachedPreview = true;
        }
        return cachedPreview;
    }

    private ItemStack createPreview() {
        if (!isComplete()) {
            return ItemStack.EMPTY;
        }
        ItemStack built = target.build(getMaterialList());
        ItemStack result = built.isEmpty() ? ItemStack.EMPTY : built.copy();
        if (result.isEmpty()) {
            return result;
        }
        ToolLevelingCompat.applyLevel(result, toolLevel);
        for (String identifier : modifiers) {
            IModifier modifier = target.resolveModifier(identifier);
            if (modifier == null) {
                continue;
            }
            int repeat = getStoredApplicationRepeat(modifier);
            for (int i = 0; i < repeat; i++) {
                try {
                    if (modifier.canApply(result.copy(), result)) {
                        modifier.apply(result);
                    }
                } catch (TinkerGuiException ignored) {
                    break;
                }
            }
        }
        applySpecialModifier(result, embossModifierId);
        applySpecialModifier(result, secondEmbossModifierId);
        applySpecialModifier(result, materialModifierId);
        rebuildPreview(result);
        HydrogenationCompat.applyPreviewData(result, target.getRequiredComponents(), embossPartIndex, embossModifierId);
        HydrogenationCompat.applyPreviewData(result, target.getRequiredComponents(), secondEmbossPartIndex, secondEmbossModifierId);
        return result;
    }

    private void refreshComputedState(String stateKey) {
        if (stateKey.equals(cachedStateKey)) {
            return;
        }
        cachedStateKey = stateKey;
        cachedPreview = ItemStack.EMPTY;
        hasCachedPreview = false;
        cachedModifierLevels.clear();
        cachedCanAddModifiers.clear();
    }

    private String buildStateKey() {
        StringBuilder builder = new StringBuilder(96 + materials.length * 16 + modifiers.size() * 16);
        builder.append(String.valueOf(target.getItem().getRegistryName())).append('|');
        builder.append(toolLevel).append('|');
        builder.append(embossPartIndex).append('|').append(nullToEmpty(embossModifierId)).append('|');
        builder.append(secondEmbossPartIndex).append('|').append(nullToEmpty(secondEmbossModifierId)).append('|');
        builder.append(nullToEmpty(materialModifierId));
        for (Material material : materials) {
            builder.append('|').append(material == null ? "" : material.getIdentifier());
        }
        for (String modifier : modifiers) {
            builder.append('|').append(nullToEmpty(modifier));
        }
        return builder.toString();
    }

    private String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private void applySpecialModifier(ItemStack result, @Nullable String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return;
        }
        IModifier modifier = target.resolveModifier(identifier);
        if (modifier == null) {
            return;
        }
        try {
            if (modifier.canApply(result.copy(), result)) {
                modifier.apply(result);
            }
        } catch (TinkerGuiException ignored) {
        }
    }

    List<Material> getMaterialList() {
        List<Material> list = new ArrayList<>(materials.length);
        for (Material material : materials) {
            list.add(material);
        }
        return list;
    }

    int getModifierLevel(String identifier) {
        String stateKey = buildStateKey();
        refreshComputedState(stateKey);
        Integer cached = cachedModifierLevels.get(identifier);
        if (cached != null) {
            return cached;
        }
        ItemStack preview = getPreviewForState(stateKey);
        if (preview.isEmpty()) {
            int count = countModifierEntries(identifier);
            cachedModifierLevels.put(identifier, count);
            return count;
        }
        IModifier modifier = target.resolveModifier(identifier);
        int level = getEffectiveModifierLevel(modifier, getModifierProgress(preview, identifier));
        int result = level > 0 ? level : countModifierEntries(identifier);
        cachedModifierLevels.put(identifier, result);
        return result;
    }

    boolean canAddModifier(IModifier modifier) {
        if (!isComplete() || modifier == null) {
            return false;
        }
        String stateKey = buildStateKey();
        refreshComputedState(stateKey);
        String identifier = modifier.getIdentifier();
        Boolean cached = cachedCanAddModifiers.get(identifier);
        if (cached != null) {
            return cached;
        }
        ItemStack preview = getPreviewForState(stateKey);
        if (preview.isEmpty()) {
            return false;
        }
        try {
            boolean result = modifier.canApply(preview.copy(), preview);
            cachedCanAddModifiers.put(identifier, result);
            return result;
        } catch (TinkerGuiException ignored) {
            cachedCanAddModifiers.put(identifier, false);
            return false;
        }
    }

    boolean addModifier(IModifier modifier) {
        if (!canAddModifier(modifier)) {
            return false;
        }
        String identifier = modifier.getIdentifier();
        if (isDiscreteLevelModifier(modifier)) {
            modifiers.add(identifier);
            return true;
        }
        int targetLevel = getEffectiveModifierLevel(modifier, getModifierProgress(getPreviewForState(buildStateKey()), identifier)) + 1;
        modifiers.add(identifier);
        for (int i = 0; i < MAX_BATCH_MODIFIER_APPLIES; i++) {
            ModifierProgress progress = getModifierProgress(getPreviewForState(buildStateKey()), identifier);
            if (!progress.hasProgress || getEffectiveModifierLevel(modifier, progress) >= targetLevel || !canAddModifier(modifier)) {
                break;
            }
            modifiers.add(identifier);
        }
        return true;
    }

    private int countModifierEntries(String identifier) {
        int count = 0;
        for (String modifier : modifiers) {
            if (identifier.equals(modifier)) {
                count++;
            }
        }
        return count;
    }

    private int getStoredApplicationRepeat(IModifier modifier) {
        MultiAspectInfo info = getMultiAspectInfo(modifier);
        return info == null || "luck".equals(modifier.getIdentifier()) ? 1 : info.countPerLevel;
    }

    private boolean isDiscreteLevelModifier(IModifier modifier) {
        return modifier != null && !"luck".equals(modifier.getIdentifier()) && getMultiAspectInfo(modifier) != null;
    }

    @Nullable
    private MultiAspectInfo getMultiAspectInfo(IModifier modifier) {
        if (modifier == null) {
            return null;
        }
        Class<?> modifierClass = modifier.getClass();
        MultiAspectInfo cached = MULTI_ASPECT_INFO_CACHE.get(modifierClass);
        if (cached != null) {
            return cached == NO_MULTI_ASPECT_INFO ? null : cached;
        }
        try {
            Field aspectsField = null;
            Class<?> type = modifierClass;
            while (type != null && aspectsField == null) {
                try {
                    aspectsField = type.getDeclaredField("aspects");
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
            if (aspectsField == null) {
                return null;
            }
            aspectsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ModifierAspect> aspects = (List<ModifierAspect>) aspectsField.get(modifier);
            if (aspects == null) {
                return null;
            }
            for (ModifierAspect aspect : aspects) {
                if (aspect == null || !aspect.getClass().getName().endsWith("ModifierAspect$MultiAspect")) {
                    continue;
                }
                Field countPerLevelField = aspect.getClass().getDeclaredField("countPerLevel");
                countPerLevelField.setAccessible(true);
                int countPerLevel = countPerLevelField.getInt(aspect);

                Field levelAspectField = aspect.getClass().getDeclaredField("levelAspect");
                levelAspectField.setAccessible(true);
                Object levelAspect = levelAspectField.get(aspect);
                if (levelAspect == null) {
                    continue;
                }
                Field maxLevelField = levelAspect.getClass().getDeclaredField("maxLevel");
                maxLevelField.setAccessible(true);
                int maxLevel = maxLevelField.getInt(levelAspect);
                MultiAspectInfo info = new MultiAspectInfo(Math.max(1, countPerLevel), Math.max(1, maxLevel));
                MULTI_ASPECT_INFO_CACHE.put(modifierClass, info);
                return info;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        MULTI_ASPECT_INFO_CACHE.put(modifierClass, NO_MULTI_ASPECT_INFO);
        return null;
    }

    private int getEffectiveModifierLevel(IModifier modifier, ModifierProgress progress) {
        if (modifier != null && "luck".equals(modifier.getIdentifier())) {
            return getLuckLevel(progress.current);
        }
        return progress.level;
    }

    private int getLuckLevel(int current) {
        int level = 0;
        while (level < 3 && current >= 60 * (level + 1) * (level + 2) / 2) {
            level++;
        }
        return level;
    }

    private ModifierProgress getModifierProgress(ItemStack stack, String identifier) {
        if (stack.isEmpty() || !TinkerUtil.hasModifier(TagUtil.getTagSafe(stack), identifier)) {
            return ModifierProgress.NONE;
        }
        NBTTagCompound tag = TinkerUtil.getModifierTag(stack, identifier);
        ModifierNBT.IntegerNBT data = ModifierNBT.readInteger(tag);
        return new ModifierProgress(data.level, data.current, data.max, data.max > 0 || data.current > 0);
    }

    private static final class ModifierProgress {
        private static final ModifierProgress NONE = new ModifierProgress(0, 0, 0, false);
        private final int level;
        private final int current;
        private final int max;
        private final boolean hasProgress;

        private ModifierProgress(int level, int current, int max, boolean hasProgress) {
            this.level = level;
            this.current = current;
            this.max = max;
            this.hasProgress = hasProgress;
        }
    }

    private static final class MultiAspectInfo {
        private final int countPerLevel;
        private final int maxLevel;

        private MultiAspectInfo(int countPerLevel, int maxLevel) {
            this.countPerLevel = countPerLevel;
            this.maxLevel = maxLevel;
        }
    }

    boolean removeModifier(String identifier) {
        int targetLevel = Math.max(0, getModifierLevel(identifier) - 1);
        boolean removed = false;
        for (int guard = 0; guard < MAX_BATCH_MODIFIER_APPLIES; guard++) {
            if (!removeLastModifierEntry(identifier)) {
                break;
            }
            removed = true;
            if (getModifierLevel(identifier) <= targetLevel) {
                break;
            }
        }
        return removed;
    }

    boolean hasEmboss() {
        return embossModifierId != null && !embossModifierId.isEmpty();
    }

    boolean hasMaterialModifier() {
        return materialModifierId != null && !materialModifierId.isEmpty();
    }

    boolean hasSecondEmboss() {
        return secondEmbossModifierId != null && !secondEmbossModifierId.isEmpty();
    }

    void setEmboss(int partIndex, @Nullable String modifierId) {
        embossPartIndex = partIndex;
        embossModifierId = modifierId;
    }

    void clearEmboss() {
        embossPartIndex = -1;
        embossModifierId = null;
        clearSecondEmboss();
    }

    void setSecondEmboss(int partIndex, @Nullable String modifierId) {
        secondEmbossPartIndex = partIndex;
        secondEmbossModifierId = modifierId;
    }

    void clearSecondEmboss() {
        secondEmbossPartIndex = -1;
        secondEmbossModifierId = null;
    }

    void setMaterialModifier(@Nullable String modifierId) {
        materialModifierId = modifierId;
    }

    void clearMaterialModifier() {
        materialModifierId = null;
    }

    private boolean removeLastModifierEntry(String identifier) {
        for (int i = modifiers.size() - 1; i >= 0; i--) {
            if (identifier.equals(modifiers.get(i))) {
                modifiers.remove(i);
                return true;
            }
        }
        return false;
    }

    private void rebuildPreview(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound root = TagUtil.getTagSafe(stack);
        try {
            if (ConArmPresence.isLoaded() && ConArmCompat.rebuildArmor(stack, root)) {
                return;
            } else if (stack.getItem() instanceof TinkersItem) {
                ToolBuilder.rebuildTool(root, (TinkersItem) stack.getItem());
            }
            stack.setTagCompound(root);
        } catch (TinkerGuiException ignored) {
        }
    }

    NBTTagCompound toTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", target.getType().name());
        tag.setString("item", String.valueOf(target.getItem().getRegistryName()));
        tag.setInteger("toolLevel", toolLevel);
        NBTTagList materialList = new NBTTagList();
        for (Material material : materials) {
            materialList.appendTag(new NBTTagString(material == null ? "" : material.getIdentifier()));
        }
        tag.setTag("materials", materialList);
        NBTTagList modifierList = new NBTTagList();
        for (String modifier : modifiers) {
            modifierList.appendTag(new NBTTagString(modifier));
        }
        tag.setTag("modifiers", modifierList);
        if (hasEmboss()) {
            tag.setString("embossModifier", embossModifierId);
            tag.setInteger("embossPart", embossPartIndex);
        }
        if (hasSecondEmboss()) {
            tag.setString("secondEmbossModifier", secondEmbossModifierId);
            tag.setInteger("secondEmbossPart", secondEmbossPartIndex);
        }
        if (hasMaterialModifier()) {
            tag.setString("materialModifier", materialModifierId);
        }
        return tag;
    }

    @Nullable
    static PlannerBlueprint fromStack(PlannerTarget target, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != target.getItem()) {
            return null;
        }
        PlannerBlueprint blueprint = new PlannerBlueprint(target);
        List<Material> materialList = TinkerUtil.getMaterialsFromTagList(TagUtil.getBaseMaterialsTagList(stack));
        for (int i = 0; i < blueprint.materials.length && i < materialList.size(); i++) {
            blueprint.materials[i] = materialList.get(i);
        }
        blueprint.toolLevel = ToolLevelingCompat.readLevel(stack);
        NBTTagList baseModifiers = TagUtil.getBaseModifiersTagList(stack);
        for (int i = 0; i < baseModifiers.tagCount(); i++) {
            String identifier = baseModifiers.getStringTagAt(i);
            if (ToolLevelingCompat.isLevelingModifier(identifier)) {
                continue;
            }
            IModifier modifier = target.resolveModifier(identifier);
            if (modifier == null) {
                continue;
            }
            if (isSecondEmbossModifier(target, modifier)) {
                blueprint.secondEmbossModifierId = identifier;
                blueprint.secondEmbossPartIndex = -1;
            } else if (isEmbossModifier(target, modifier)) {
                blueprint.embossModifierId = identifier;
                blueprint.embossPartIndex = -1;
            } else if (isMaterialModifier(target, modifier)) {
                blueprint.materialModifierId = identifier;
            } else {
                blueprint.modifiers.add(identifier);
            }
        }
        return blueprint;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlannerBlueprint)) {
            return false;
        }
        return toTag().equals(((PlannerBlueprint) obj).toTag());
    }

    @Override
    public int hashCode() {
        return toTag().hashCode();
    }

    static PlannerBlueprint fromTag(NBTTagCompound tag, List<? extends PlannerTarget> targets) {
        ResourceLocation itemId = new ResourceLocation(tag.getString("item"));
        for (PlannerTarget target : targets) {
            if (itemId.equals(target.getItem().getRegistryName())) {
                PlannerBlueprint blueprint = new PlannerBlueprint(target);
                NBTTagList materialList = tag.getTagList("materials", 8);
                for (int i = 0; i < materialList.tagCount() && i < blueprint.materials.length; i++) {
                    String identifier = materialList.getStringTagAt(i);
                    if (!identifier.isEmpty()) {
                        blueprint.materials[i] = slimeknights.tconstruct.library.TinkerRegistry.getMaterial(identifier);
                    }
                }
                blueprint.toolLevel = tag.hasKey("toolLevel", 3) ? tag.getInteger("toolLevel") : ToolLevelingCompat.getDefaultDisplayLevel();
                NBTTagList modifierList = tag.getTagList("modifiers", 8);
                for (int i = 0; i < modifierList.tagCount(); i++) {
                    String identifier = modifierList.getStringTagAt(i);
                    IModifier modifier = target.resolveModifier(identifier);
                    if (modifier == null) {
                        continue;
                    }
                    if (isSecondEmbossModifier(target, modifier)) {
                        blueprint.secondEmbossModifierId = identifier;
                        blueprint.secondEmbossPartIndex = tag.hasKey("secondEmbossPart", 3) ? tag.getInteger("secondEmbossPart") : -1;
                    } else if (isEmbossModifier(target, modifier)) {
                        blueprint.embossModifierId = identifier;
                        blueprint.embossPartIndex = tag.hasKey("embossPart", 3) ? tag.getInteger("embossPart") : -1;
                    } else if (isMaterialModifier(target, modifier)) {
                        blueprint.materialModifierId = identifier;
                    } else {
                        blueprint.modifiers.add(identifier);
                    }
                }
                if (tag.hasKey("embossModifier", 8)) {
                    String identifier = tag.getString("embossModifier");
                    if (target.resolveModifier(identifier) != null) {
                        blueprint.embossModifierId = identifier;
                        blueprint.embossPartIndex = tag.getInteger("embossPart");
                    }
                }
                if (tag.hasKey("secondEmbossModifier", 8)) {
                    String identifier = tag.getString("secondEmbossModifier");
                    if (target.resolveModifier(identifier) != null) {
                        blueprint.secondEmbossModifierId = identifier;
                        blueprint.secondEmbossPartIndex = tag.getInteger("secondEmbossPart");
                    }
                }
                if (tag.hasKey("materialModifier", 8)) {
                    String identifier = tag.getString("materialModifier");
                    if (target.resolveModifier(identifier) != null) {
                        blueprint.materialModifierId = identifier;
                    }
                }
                return blueprint;
            }
        }
        return null;
    }

    private static boolean isEmbossModifier(PlannerTarget target, IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase();
        if (target.getType() == PlannerTarget.TargetType.ARMOR) {
            return className.contains("modextraarmortrait") && !className.contains("modextraarmortrait2") && !className.contains("display");
        }
        return className.contains("modextratrait") && !className.contains("modextratrait2") && !className.contains("display");
    }

    private static boolean isSecondEmbossModifier(PlannerTarget target, IModifier modifier) {
        return MoreTConCompat.isSecondEmbossModifier(target.getType(), modifier);
    }

    private static boolean isMaterialModifier(PlannerTarget target, IModifier modifier) {
        String className = modifier.getClass().getName().toLowerCase();
        if (target.getType() == PlannerTarget.TargetType.ARMOR) {
            return className.contains("modpolished") && !className.contains("display");
        }
        return className.contains("modfortify") && !className.contains("display");
    }
}
