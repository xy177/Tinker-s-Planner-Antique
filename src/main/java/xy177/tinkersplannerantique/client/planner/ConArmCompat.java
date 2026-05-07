package xy177.tinkersplannerantique.client.planner;

import java.util.Collection;
import java.util.List;

import c4.conarm.common.ConstructsRegistry;
import c4.conarm.lib.ArmoryRegistry;
import c4.conarm.common.inventory.ContainerArmorStation;
import c4.conarm.common.inventory.SlotArmorStationIn;
import c4.conarm.common.inventory.SlotArmorStationOut;
import c4.conarm.lib.armor.ArmorCore;
import c4.conarm.lib.modifiers.ArmorModifier;
import c4.conarm.lib.modifiers.ArmorModifierTrait;
import c4.conarm.lib.tinkering.ArmorBuilder;
import c4.conarm.lib.tinkering.TinkersArmor;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.modifiers.TinkerGuiException;

final class ConArmCompat {

    static final String MODID = "conarm";
    static final String CORE = "core";
    static final String PLATES = "plates";
    static final String TRIM = "trim";

    private ConArmCompat() {
    }

    static boolean isLoaded() {
        return ConArmPresence.isLoaded();
    }

    static boolean isPlannerArmor(ItemStack stack) {
        return isLoaded() && !stack.isEmpty() && stack.getItem() instanceof TinkersArmor;
    }

    static boolean isArmorItem(ItemStack stack) {
        return isPlannerArmor(stack);
    }

    static boolean rebuildArmor(ItemStack stack, NBTTagCompound root) {
        if (!isPlannerArmor(stack)) {
            return false;
        }
        try {
            ArmorBuilder.rebuildArmor(root, (TinkersArmor) stack.getItem());
            stack.setTagCompound(root);
            return true;
        } catch (TinkerGuiException ignored) {
            return true;
        }
    }

    static ItemStack getPolishingKitStack(Material material) {
        if (!isLoaded() || ConstructsRegistry.polishingKit == null) {
            return ItemStack.EMPTY;
        }
        return material == null ? new ItemStack(ConstructsRegistry.polishingKit) : ConstructsRegistry.polishingKit.getItemstackWithMaterial(material);
    }

    static boolean canUsePolishingMaterial(Material material) {
        return isLoaded() && material != null && ConstructsRegistry.polishingKit != null && ConstructsRegistry.polishingKit.canUseMaterial(material) && material.hasStats(PLATES);
    }

    static List<List<ItemStack>> getModifierItems(IModifier modifier) {
        if (!isLoaded() || modifier == null) {
            return null;
        }
        if (modifier instanceof ArmorModifier) {
            return ((ArmorModifier) modifier).getItems();
        }
        if (modifier instanceof ArmorModifierTrait) {
            return ((ArmorModifierTrait) modifier).getItems();
        }
        return null;
    }

    static Collection<IModifier> getAllArmorModifiers() {
        return ArmoryRegistry.getAllArmorModifiers();
    }

    static boolean selectArmorTarget(Container container, ItemStack target, int activeSlots) {
        if (!isLoaded() || !(container instanceof ContainerArmorStation) || !(target.getItem() instanceof ArmorCore)) {
            return false;
        }
        ((ContainerArmorStation) container).setArmorSelection((ArmorCore) target.getItem(), activeSlots);
        return true;
    }

    static boolean isArmorInputSlot(Slot slot) {
        return isLoaded() && slot instanceof SlotArmorStationIn;
    }

    static boolean isArmorStationSlot(Slot slot) {
        return isLoaded() && (slot instanceof SlotArmorStationIn || slot instanceof SlotArmorStationOut);
    }
}
