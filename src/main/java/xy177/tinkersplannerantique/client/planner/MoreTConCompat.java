package xy177.tinkersplannerantique.client.planner;

import java.util.Locale;

import net.minecraftforge.fml.common.Loader;
import slimeknights.tconstruct.library.modifiers.IModifier;

final class MoreTConCompat {

    static final String MODID = "moretcon";
    private static final String SECOND_EMBOSS_PREFIX = "moretcon.extratrait2";

    private MoreTConCompat() {
    }

    static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    static boolean isSecondEmbossModifier(PlannerTarget.TargetType type, IModifier modifier) {
        if (modifier == null) {
            return false;
        }
        String identifier = modifier.getIdentifier();
        String idLower = identifier == null ? "" : identifier.toLowerCase(Locale.ROOT);
        String classLower = modifier.getClass().getName().toLowerCase(Locale.ROOT);
        if (classLower.contains("display")) {
            return false;
        }
        if (type == PlannerTarget.TargetType.ARMOR) {
            return (idLower.startsWith(SECOND_EMBOSS_PREFIX) || classLower.contains("modextraarmortrait2")) && !idLower.endsWith("_armor");
        }
        return idLower.startsWith(SECOND_EMBOSS_PREFIX) || classLower.contains("modextratrait2");
    }
}
