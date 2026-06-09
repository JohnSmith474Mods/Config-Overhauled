package johnsmith.configoverhauled.client.event;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.client.gui.registry.ConfigWidgetRegistry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class NeoForgeClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ConfigWidgetRegistry::registerWidgets);
    }
}