package xy177.tinkersplannerantique;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import xy177.tinkersplannerantique.client.planner.PlannerNetwork;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        PlannerConfig.init(event.getModConfigurationDirectory());
        PlannerNetwork.init();
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
