package xy177.tinkersplannerantique.client.planner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import c4.conarm.lib.ArmoryRegistry;
import c4.conarm.lib.armor.ArmorCore;
import c4.conarm.lib.client.ArmorBuildGuiInfo;
import c4.conarm.lib.modifiers.ArmorModifier;
import c4.conarm.lib.modifiers.ArmorModifierTrait;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.modifiers.ModifierTrait;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.tools.modifiers.ToolModifier;

final class ArmorPlannerTarget implements PlannerTarget {

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
        String className = modifier.getClass().getName().toLowerCase();
        if (className.contains("modpolished") && !className.contains("display")) {
            return true;
        }
        if (className.contains("modextraarmortrait") && !className.contains("display")) {
            return true;
        }
        if (modifier.hasItemsToApplyWith()) {
            return true;
        }
        List<List<ItemStack>> items = null;
        if (modifier instanceof ArmorModifier) {
            items = ((ArmorModifier) modifier).getItems();
        } else if (modifier instanceof ArmorModifierTrait) {
            items = ((ArmorModifierTrait) modifier).getItems();
        } else if (modifier instanceof ToolModifier) {
            items = ((ToolModifier) modifier).getItems();
        } else if (modifier instanceof ModifierTrait) {
            items = ((ModifierTrait) modifier).getItems();
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

    @Override
    public ItemStack build(List<Material> materials) {
        return armor.buildItem(materials);
    }

    @Override
    public IModifier resolveModifier(String identifier) {
        IModifier modifier = ArmoryRegistry.getArmorModifier(identifier);
        return modifier != null ? modifier : TinkerRegistry.getModifier(identifier);
    }
}
