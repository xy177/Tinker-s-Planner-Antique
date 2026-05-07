package xy177.tinkersplannerantique.client.planner;

import net.minecraftforge.fml.common.Loader;

final class ConArmPresence {

    private static final String MODID = "conarm";

    private ConArmPresence() {
    }

    static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }
}
