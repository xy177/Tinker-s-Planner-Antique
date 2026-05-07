package xy177.tinkersplannerantique.client.planner;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.Loader;
import slimeknights.tconstruct.library.materials.MaterialTypes;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.library.utils.TinkerUtil;

final class HydrogenationCompat {

    static final String MODID = "hydrogenation_tinker";
    private static final String TAG_PART_TYPE = "part_type";
    private static Method reloadExtraTagsMethod;

    private HydrogenationCompat() {
    }

    static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    static void applyPreviewData(ItemStack stack, List<PartMaterialType> partTypes, int embossPartIndex, String embossModifierId) {
        if (!isLoaded() || stack.isEmpty()) {
            return;
        }
        if (embossModifierId != null && !embossModifierId.isEmpty() && embossPartIndex >= 0 && embossPartIndex < partTypes.size()) {
            NBTTagList types = new NBTTagList();
            for (String statType : getUsedStatTypes(partTypes.get(embossPartIndex))) {
                types.appendTag(new NBTTagString(statType));
            }
            TinkerUtil.getModifierTag(stack, embossModifierId).setTag(TAG_PART_TYPE, types);
        }
        try {
            Method reload = getReloadMethod();
            if (reload != null) {
                reload.invoke(null, partTypes, TagUtil.getTagSafe(stack));
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method getReloadMethod() {
        if (reloadExtraTagsMethod != null) {
            return reloadExtraTagsMethod;
        }
        try {
            Class<?> shardData = Class.forName("threshold.hydrogenation_tinker.materials.ShardData");
            reloadExtraTagsMethod = shardData.getMethod("reloadExtraTags", List.class, net.minecraft.nbt.NBTTagCompound.class);
        } catch (ReflectiveOperationException ignored) {
        }
        return reloadExtraTagsMethod;
    }

    private static List<String> getUsedStatTypes(PartMaterialType partType) {
        java.util.ArrayList<String> statTypes = new java.util.ArrayList<>();
        addUsed(statTypes, partType, MaterialTypes.HEAD);
        addUsed(statTypes, partType, MaterialTypes.HANDLE);
        addUsed(statTypes, partType, MaterialTypes.EXTRA);
        addUsed(statTypes, partType, MaterialTypes.BOW);
        addUsed(statTypes, partType, MaterialTypes.BOWSTRING);
        addUsed(statTypes, partType, MaterialTypes.SHAFT);
        addUsed(statTypes, partType, MaterialTypes.FLETCHING);
        addUsed(statTypes, partType, MaterialTypes.PROJECTILE);
        if (ConArmPresence.isLoaded()) {
            addUsed(statTypes, partType, ConArmCompat.CORE);
            addUsed(statTypes, partType, ConArmCompat.PLATES);
            addUsed(statTypes, partType, ConArmCompat.TRIM);
        }
        return statTypes;
    }

    private static void addUsed(List<String> out, PartMaterialType partType, String statType) {
        if (partType.usesStat(statType)) {
            out.add(statType);
        }
    }
}
