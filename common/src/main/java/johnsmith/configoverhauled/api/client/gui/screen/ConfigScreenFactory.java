package johnsmith.configoverhauled.api.client.gui.screen;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.impl.client.gui.screen.DefaultConfigScreen;

import net.minecraft.client.gui.screens.Screen;

public final class ConfigScreenFactory {
    /**
     * Constructs the default configuration screen implementation.
     *
     * @param parent  The parent screen to return to upon closing.
     * @param manager The configuration manager supplying the data model.
     * @return A constructed screen satisfying the ConfigScreen contract.
     */
    public static ConfigScreen createDefault(Screen parent, ConfigManager manager) {
        return new DefaultConfigScreen(parent, manager);
    }

    /**
     * Constructs the default configuration screen implementation.
     *
     * @param parent         The parent screen to return to upon closing.
     * @param manager        The configuration manager supplying the data model.
     * @param widgetRegistry The widget registry supplying the widget factories.
     * @return A constructed screen satisfying the ConfigScreen contract.
     */
    public static ConfigScreen createWithRegistry(Screen parent, ConfigManager manager, WidgetRegistry widgetRegistry) {
        return new DefaultConfigScreen(parent, manager, widgetRegistry);
    }
}