package johnsmith.configoverhauled;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Constants.MOD_ID)
public class NeoForge {
    public NeoForge(ModContainer modContainer) {
        Common.init();

        Config.MANAGER.init(FMLPaths.CONFIGDIR.get());

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parentScreen) ->
                    Config.MANAGER.createScreen(parentScreen)
            );
        }
    }
}