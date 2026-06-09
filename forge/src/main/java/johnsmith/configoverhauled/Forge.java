package johnsmith.configoverhauled;

import johnsmith.configoverhauled.client.event.ForgeClientEvents;
import johnsmith.configoverhauled.client.event.ForgeClientModEvents;
import johnsmith.configoverhauled.event.ForgeGameEvents;
import johnsmith.configoverhauled.network.ForgeNetworkRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Constants.MOD_ID)
public class Forge {

    public Forge(FMLJavaModLoadingContext context) {

        var modBusGroup = context.getModBusGroup();

        Common.init();
        Config.MANAGER.init(FMLPaths.CONFIGDIR.get());

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(ForgeNetworkRegistration::setup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientModEvents::onClientSetup);

            context.registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) ->
                            Config.MANAGER.createScreen(parentScreen)
                    )
            );

            ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(ForgeClientEvents::onClientDisconnect);
            RegisterClientCommandsEvent.BUS.addListener(ForgeClientEvents::onClientCommands);
        }

        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ForgeGameEvents::onPlayerJoin);
        ServerStartingEvent.BUS.addListener(ForgeGameEvents::onServerStarting);
        ServerStoppingEvent.BUS.addListener(ForgeGameEvents::onServerStopping);
    }
}