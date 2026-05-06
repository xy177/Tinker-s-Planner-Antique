package xy177.tinkersplannerantique.client.planner;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;

interface PlannerTarget {

    enum TargetType {
        TOOL,
        ARMOR
    }

    TargetType getType();

    Item getItem();

    ItemStack getRenderStack();

    String getDisplayName();

    List<PartMaterialType> getRequiredComponents();

    List<IModifier> getAvailableModifiers();

    ItemStack build(List<Material> materials);

    IModifier resolveModifier(String identifier);
}
