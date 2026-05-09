package xy177.tinkersplannerantique;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = TinkersPlannerAntique.MODID,
    name = TinkersPlannerAntique.NAME,
    version = TinkersPlannerAntique.VERSION,
    dependencies = TinkersPlannerAntique.DEPENDENCIES,
    acceptedMinecraftVersions = TinkersPlannerAntique.ACCEPTED_MC_VERSIONS
)
public class TinkersPlannerAntique {

    public static final String MODID = "tinkersplannerantique";
    public static final String NAME = "Tinkers Planner Antique";
    public static final String VERSION = "1.0.3";
    public static final String ACCEPTED_MC_VERSIONS = "[1.12,1.13)";
    public static final String DEPENDENCIES =
        "required-after:tconstruct@[1.12.2-2.13.0.183,);after:conarm";

    @Mod.Instance(MODID)
    public static TinkersPlannerAntique instance;

    @SidedProxy(
        clientSide = "xy177.tinkersplannerantique.ClientProxy",
        serverSide = "xy177.tinkersplannerantique.CommonProxy"
    )
    public static CommonProxy proxy;

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.preInit(event);
        logger.info("{} is preparing its startup pipeline.", NAME);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("{} detected required dependencies and entered init.", NAME);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        logger.info("{} finished basic scaffolding.", NAME);
    }
}
