package johnsmith.configoverhauled;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Constants.MOD_ID)
public class Forge {
    public Forge() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Common.init();

        Config.MANAGER.init(FMLPaths.CONFIGDIR.get());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) ->
                            Config.MANAGER.createScreen(parentScreen)
                    )
            );
        }
    }
}