package xy177.tinkersplannerantique.client.planner;

import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.Loader;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.library.utils.Tags;
import slimeknights.tconstruct.library.utils.TinkerUtil;

final class ToolLevelingCompat {

    static final String MODID = "tinkertoolleveling";
    static final String TOOL_MODIFIER_ID = "toolleveling";
    static final String ARMOR_MODIFIER_ID = "leveling_armor";
    private static final String TAG_LEVEL = "level";
    private static final String TAG_XP = "xp";
    private static final String TAG_BONUS_MODIFIERS = "bonus_modifiers";
    private static final int COLOR = 0xFFFFFF;

    private ToolLevelingCompat() {
    }

    static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    static int readLevel(ItemStack stack) {
        if (!isLoaded() || stack.isEmpty()) {
            return 0;
        }
        String modifierId = getModifierId(stack);
        if (modifierId == null || !TinkerUtil.hasModifier(TagUtil.getTagSafe(stack), modifierId)) {
            return 1;
        }
        return Math.max(1, TinkerUtil.getModifierTag(stack, modifierId).getInteger(TAG_LEVEL));
    }

    static void applyLevel(ItemStack stack, int displayLevel) {
        if (!isLoaded() || stack.isEmpty()) {
            return;
        }
        String modifierId = getModifierId(stack);
        if (modifierId == null) {
            return;
        }
        int normalizedLevel = Math.max(1, displayLevel);
        int bonusModifiers = Math.max(0, normalizedLevel - 1);
        NBTTagCompound root = TagUtil.getTagSafe(stack);
        NBTTagList baseModifiers = TagUtil.getBaseModifiersTagList(root);

        if (isArmor(stack) && containsString(baseModifiers, TOOL_MODIFIER_ID)) {
            removeString(baseModifiers, TOOL_MODIFIER_ID);
        }
        if (!containsString(baseModifiers, modifierId)) {
            baseModifiers.appendTag(new NBTTagString(modifierId));
        }
        TagUtil.setBaseModifiersTagList(root, baseModifiers);

        NBTTagList modifiers = TagUtil.getModifiersTagList(root);
        if (isArmor(stack)) {
            removeCompound(modifiers, TOOL_MODIFIER_ID);
        }
        int index = TinkerUtil.getIndexInCompoundList(modifiers, modifierId);
        NBTTagCompound modifierTag = index >= 0 ? modifiers.getCompoundTagAt(index) : new NBTTagCompound();
        modifierTag.setString("identifier", modifierId);
        modifierTag.setInteger("color", COLOR);
        modifierTag.setInteger(TAG_LEVEL, normalizedLevel);
        modifierTag.setInteger(TAG_XP, 0);
        modifierTag.setInteger(TAG_BONUS_MODIFIERS, bonusModifiers);
        if (index >= 0) {
            modifiers.set(index, modifierTag);
        } else {
            modifiers.appendTag(modifierTag);
        }
        TagUtil.setModifiersTagList(root, modifiers);
        NBTTagCompound toolTag = TagUtil.getToolTag(root);
        toolTag.setInteger(Tags.FREE_MODIFIERS, Math.max(0, toolTag.getInteger(Tags.FREE_MODIFIERS) + bonusModifiers));
        TagUtil.setToolTag(root, toolTag);
        stack.setTagCompound(root);
    }

    static boolean canLevelUp(ItemStack stack, int displayLevel) {
        if (!isLoaded()) {
            return false;
        }
        int normalizedLevel = Math.max(1, displayLevel);
        try {
            if (isArmor(stack)) {
                Class<?> clazz = Class.forName("c4.conarm.integrations.tinkertoolleveling.ModArmorLeveling");
                Method method = clazz.getMethod("canLevelUp", int.class);
                Object result = method.invoke(null, normalizedLevel);
                return result instanceof Boolean && (Boolean) result;
            }
            Class<?> configClass = Class.forName("slimeknights.toolleveling.config.Config");
            Method method = configClass.getMethod("canLevelUp", int.class);
            Object result = method.invoke(null, normalizedLevel);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    static int getDefaultDisplayLevel() {
        return isLoaded() ? 1 : 0;
    }

    static boolean isLevelingModifier(String identifier) {
        return TOOL_MODIFIER_ID.equals(identifier) || ARMOR_MODIFIER_ID.equals(identifier);
    }

    private static String getModifierId(ItemStack stack) {
        return isArmor(stack) ? ARMOR_MODIFIER_ID : TOOL_MODIFIER_ID;
    }

    private static boolean isArmor(ItemStack stack) {
        return ConArmPresence.isLoaded() && ConArmCompat.isArmorItem(stack);
    }

    private static boolean containsString(NBTTagList list, String identifier) {
        for (int i = 0; i < list.tagCount(); i++) {
            if (identifier.equals(list.getStringTagAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static void removeString(NBTTagList list, String identifier) {
        for (int i = list.tagCount() - 1; i >= 0; i--) {
            if (identifier.equals(list.getStringTagAt(i))) {
                list.removeTag(i);
            }
        }
    }

    private static void removeCompound(NBTTagList list, String identifier) {
        for (int i = list.tagCount() - 1; i >= 0; i--) {
            if (identifier.equals(list.getCompoundTagAt(i).getString("identifier"))) {
                list.removeTag(i);
            }
        }
    }
}
