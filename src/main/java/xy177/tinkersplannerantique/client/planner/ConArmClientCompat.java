package xy177.tinkersplannerantique.client.planner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import c4.conarm.client.gui.GuiArmorStation;
import c4.conarm.lib.ArmoryRegistry;
import c4.conarm.lib.ArmoryRegistryClient;
import c4.conarm.lib.armor.ArmorCore;
import c4.conarm.lib.client.ArmorBuildGuiInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.tools.modifiers.ToolModifier;

final class ConArmClientCompat {

    private ConArmClientCompat() {
    }

    static boolean isArmorStationGui(GuiScreen gui) {
        return ConArmPresence.isLoaded() && gui instanceof GuiArmorStation;
    }

    static boolean isArmorForgeGui(GuiScreen gui) {
        if (!isArmorStationGui(gui)) {
            return false;
        }
        try {
            Object container = ((GuiArmorStation) gui).inventorySlots;
            Field field = container.getClass().getDeclaredField("isUpgraded");
            field.setAccessible(true);
            return field.getBoolean(container);
        } catch (Exception ignored) {
            return false;
        }
    }

    static PlannerScreen createArmorScreen(GuiScreen parent) {
        PlannerScreen screen = new PlannerScreen(parent, PlannerTarget.TargetType.ARMOR, getArmorTargets());
        screen.importFromParent();
        return screen;
    }

    static List<ArmorPlannerTarget> getArmorTargets() {
        if (!ConArmPresence.isLoaded()) {
            return Collections.emptyList();
        }
        List<ArmorPlannerTarget> targets = new ArrayList<>();
        for (ArmorCore armor : ArmoryRegistry.getArmorCrafting()) {
            ArmorBuildGuiInfo info = ArmoryRegistryClient.getArmorBuildInfoForArmor(armor);
            if (info != null) {
                targets.add(new ArmorPlannerTarget(armor, info));
            }
        }
        return targets;
    }

    static void openArmorPlanner(GuiScreen gui) {
        if (isArmorStationGui(gui)) {
            Minecraft.getMinecraft().displayGuiScreen(createArmorScreen(gui));
        }
    }

    static List<int[]> getPartPositions(PlannerTarget target) {
        if (!(target instanceof ArmorPlannerTarget)) {
            return Collections.emptyList();
        }
        List<int[]> positions = new ArrayList<>();
        for (org.lwjgl.util.Point point : ((ArmorPlannerTarget) target).getGuiInfo().positions) {
            positions.add(new int[] { point.getX(), point.getY() });
        }
        return positions;
    }

    static ItemStack getParentStack(GuiScreen parent) {
        if (!isArmorStationGui(parent)) {
            return ItemStack.EMPTY;
        }
        return ((GuiArmorStation) parent).inventorySlots.getSlot(0).getStack();
    }

    static boolean warn(GuiScreen screen, String message) {
        if (!isArmorStationGui(screen)) {
            return false;
        }
        ((GuiArmorStation) screen).warning(message);
        return true;
    }

    static boolean isArmorOutputSlot(Slot slot) {
        return ConArmPresence.isLoaded() && slot != null && "c4.conarm.common.inventory.SlotArmorStationOut".equals(slot.getClass().getName());
    }

    static final class ArmorPlannerTarget implements PlannerTarget {
        private final ArmorCore armor;
        private final ArmorBuildGuiInfo guiInfo;
        private List<IModifier> modifiers;

        ArmorPlannerTarget(ArmorCore armor, ArmorBuildGuiInfo guiInfo) {
            this.armor = armor;
            this.guiInfo = guiInfo;
        }

        ArmorBuildGuiInfo getGuiInfo() {
            return guiInfo;
        }

        @Override
        public TargetType getType() {
            return TargetType.ARMOR;
        }

        @Override
        public Item getItem() {
            return armor;
        }

        @Override
        public ItemStack getRenderStack() {
            return guiInfo.armor;
        }

        @Override
        public String getDisplayName() {
            return new ItemStack(armor).getDisplayName();
        }

        @Override
        public List<PartMaterialType> getRequiredComponents() {
            return armor.getRequiredComponents();
        }

        @Override
        public List<IModifier> getAvailableModifiers() {
            if (modifiers == null) {
                Map<String, IModifier> byId = new LinkedHashMap<>();
                for (IModifier modifier : ArmoryRegistry.getAllArmorModifiers()) {
                    if (modifier != null && hasApplicationItems(modifier) && !modifier.isHidden()) {
                        byId.putIfAbsent(modifier.getIdentifier(), modifier);
                    }
                }
                modifiers = new ArrayList<>(byId.values());
            }
            return modifiers;
        }

        private boolean hasApplicationItems(IModifier modifier) {
            String className = modifier.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            if (className.contains("modpolished") && !className.contains("display")) {
                return true;
            }
            if (className.contains("modextraarmortrait") && !className.contains("display")) {
                return true;
            }
            if (modifier.hasItemsToApplyWith()) {
                return true;
            }
            List<List<ItemStack>> items = ConArmCompat.getModifierItems(modifier);
            if (items == null) {
                items = getToolModifierItems(modifier);
            }
            if (items == null) {
                return false;
            }
            for (List<ItemStack> group : items) {
                if (group != null && !group.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        private List<List<ItemStack>> getToolModifierItems(IModifier modifier) {
            if (modifier instanceof ToolModifier) {
                return ((ToolModifier) modifier).getItems();
            }
            if (modifier instanceof slimeknights.tconstruct.library.modifiers.ModifierTrait) {
                return ((slimeknights.tconstruct.library.modifiers.ModifierTrait) modifier).getItems();
            }
            return null;
        }

        @Override
        public ItemStack build(List<Material> materials) {
            return armor.buildItem(materials);
        }

        @Override
        public IModifier resolveModifier(String identifier) {
            IModifier modifier = ArmoryRegistry.getArmorModifier(identifier);
            return modifier != null ? modifier : slimeknights.tconstruct.library.TinkerRegistry.getModifier(identifier);
        }
    }
}
