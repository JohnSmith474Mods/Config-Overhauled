package johnsmith.configoverhauled.api.client.gui.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.factory.WidgetFactory;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;

import net.minecraft.client.Minecraft;

/**
 * A client-side registry responsible for mapping {@link Property} types to their
 * corresponding {@link WidgetFactory} implementations.
 * <p>
 * This registry securely decouples the common configuration data model from the client-side
 * UI rendering logic, ensuring dedicated servers do not attempt to load GUI classes.
 */
public interface WidgetRegistry {

    /**
     * Binds a factory to generate widgets for a specific property type.
     *
     * @param propertyClass The class of the property.
     * @param factory       The factory responsible for creating the widget for this property type.
     * @param <P>           The property type.
     */
    <P extends Property<?>> void register(Class<P> propertyClass, WidgetFactory<? super P> factory);

    /**
     * Looks up the registered factory for the given property's type and constructs a new UI entry.
     *
     * @param property The configuration property to create a widget for.
     * @param screen   The configuration screen hosting the widget.
     * @param mc       The active Minecraft client instance.
     * @param callback A callback triggered when the widget updates the property's value.
     * @param <T>      The underlying data type of the property.
     * @return A newly constructed {@link ConfigEntry}.
     * @throws IllegalStateException if no factory is registered for the property's class.
     */
    <T> ConfigEntry createEntry(Property<T> property, ConfigScreen screen, Minecraft mc, Runnable callback);
}