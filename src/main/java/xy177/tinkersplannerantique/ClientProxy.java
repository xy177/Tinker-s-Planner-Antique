package xy177.tinkersplannerantique;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import xy177.tinkersplannerantique.client.planner.PlannerClientCommand;
import xy177.tinkersplannerantique.client.planner.PlannerClientEvents;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        PlannerClientEvents.init();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientCommandHandler.instance.registerCommand(new PlannerClientCommand());
    }
}
