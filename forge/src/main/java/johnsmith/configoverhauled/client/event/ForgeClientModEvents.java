package johnsmith.configoverhauled.client.event;

import johnsmith.configoverhauled.impl.client.gui.registry.ConfigWidgetRegistry;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ForgeClientModEvents {

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ConfigWidgetRegistry::registerWidgets);
    }
}