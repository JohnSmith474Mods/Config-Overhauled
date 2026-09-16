package johnsmith.configoverhauled.api.client.gui.factory;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;

/**
 * A functional interface responsible for constructing a UI widget ({@link ConfigEntry})
 * that represents and interacts with a specific {@link Property}.
 *
 * @param <P> The specific type of property this factory supports.
 */
@FunctionalInterface
public interface WidgetFactory<P extends Property<?>> {

    /**
     * Creates a new configuration entry widget.
     *
     * @param property       The configuration property to be visually represented and modified.
     * @param parentScreen   The parent configuration screen hosting this entry.
     * @param minecraft      The active Minecraft client instance.
     * @param onValueChanged A callback to be invoked whenever the widget modifies the property's value.
     * @return A newly constructed {@link ConfigEntry} bound to the provided property.
     */
    ConfigEntry create(P property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged);
}