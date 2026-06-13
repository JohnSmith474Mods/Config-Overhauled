package johnsmith.configoverhauled.api.client.gui.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;

public interface WidgetRegistry {
    <T> ConfigEntry createEntry(Property<T> property, ConfigScreen screen, Minecraft mc, Runnable callback);
}