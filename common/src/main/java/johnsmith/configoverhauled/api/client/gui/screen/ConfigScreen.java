package johnsmith.configoverhauled.api.client.gui.screen;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.factory.WidgetFactory;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

/**
 * Represents the core API contract for a configuration screen.
 * <p>
 * Provides standardized methods for UI widgets to interact with the screen's lifecycle,
 * render cycle, and encapsulated widget generation logic.
 */
public interface ConfigScreen {

    /**
     * Flags the configuration state as modified, triggering network syncs or reload alerts on close.
     */
    void markLevelConfigModified();

    /**
     * Passes a tooltip to the screen to be rendered at the highest Z-index during the render cycle.
     * @param tooltip The formatted lines of text to display.
     */
    void setDeferredTooltip(List<FormattedCharSequence> tooltip);

    /**
     * Dynamically resolves and constructs a UI widget for the specified property
     * using the screen's internal {@link johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry}.
     *
     * @param property The configuration property to create a widget for.
     * @param <T>      The underlying data type of the property.
     * @return A newly constructed {@link ConfigEntry} ready to be appended to the screen.
     */
    <T> ConfigEntry createEntry(Property<T> property);

    /**
     * Forces an evaluation of the global "Reset All" button state.
     * Called whenever a child widget modifies its value.
     */
    void updateMasterResetButton();

    /**
     * Registers a custom widget factory for a specific property type.
     * Overrides the default widget if one already exists for the provided class.
     *
     * @param propertyClass The class of the property.
     * @param factory       The factory responsible for creating the widget.
     * @param <P>           The property type.
     */
    <P extends Property<?>> void registerWidget(Class<P> propertyClass, WidgetFactory<? super P> factory);

    /**
     * Replaces the entire widget registry used by this screen.
     * Must be called before the screen initializes its internal lists.
     *
     * @param registry The custom registry implementation to use.
     */
    void setWidgetRegistry(WidgetRegistry registry);
}