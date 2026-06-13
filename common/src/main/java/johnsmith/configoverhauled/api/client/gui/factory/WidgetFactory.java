package johnsmith.configoverhauled.api.client.gui.factory;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface WidgetFactory<P extends Property<?>> {
    ConfigEntry create(P property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged);
}