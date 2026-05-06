package xy177.tinkersplannerantique.client.planner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.client.ToolBuildGuiInfo;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.tools.ToolCore;

final class ToolPlannerTarget implements PlannerTarget {

    private final ToolCore tool;
    private final ToolBuildGuiInfo guiInfo;
    private List<IModifier> modifiers;

    ToolPlannerTarget(ToolCore tool, ToolBuildGuiInfo guiInfo) {
        this.tool = tool;
        this.guiInfo = guiInfo;
    }

    ToolBuildGuiInfo getGuiInfo() {
        return guiInfo;
    }

    @Override
    public TargetType getType() {
        return TargetType.TOOL;
    }

    @Override
    public Item getItem() {
        return tool;
    }

    @Override
    public ItemStack getRenderStack() {
        return guiInfo.tool;
    }

    @Override
    public String getDisplayName() {
        return new ItemStack(tool).getDisplayName();
    }

    @Override
    public List<PartMaterialType> getRequiredComponents() {
        return tool.getRequiredComponents();
    }

    @Override
    public List<IModifier> getAvailableModifiers() {
        if (modifiers == null) {
            modifiers = new ArrayList<>();
            for (IModifier modifier : TinkerRegistry.getAllModifiers()) {
                if (modifier != null && modifier.hasItemsToApplyWith() && !modifier.isHidden()) {
                    modifiers.add(modifier);
                }
            }
        }
        return modifiers;
    }

    @Override
    public ItemStack build(List<Material> materials) {
        return tool.buildItem(materials);
    }

    @Override
    public IModifier resolveModifier(String identifier) {
        return TinkerRegistry.getModifier(identifier);
    }
}
