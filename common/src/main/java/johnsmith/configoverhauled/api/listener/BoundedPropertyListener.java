package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

/**
 * Abstract base implementation for dynamic configuration property listeners that enforce value constraints.
 * Extends standard listener functionality to support properties requiring minimum and maximum boundaries.
 *
 * @param <T> The underlying comparable data type monitored by this listener.
 */
public abstract class BoundedPropertyListener<T> extends AbstractPropertyListener<T> {

    /** The minimum permissible value for the dynamically generated property. */
    protected final T lowerBound;

    /** The maximum permissible value for the dynamically generated property. */
    protected final T upperBound;

    /**
     * Constructs a state listener for a bounded dynamic configuration property.
     * Instantiation triggers an immediate and eager resolution of the target property.
     *
     * @param description    The structural and organizational metadata for the target property.
     * @param typeDefinition The registry definition mapping the required data type.
     * @param defaultValue   The default value assigned upon initial property generation.
     * @param lowerBound     The absolute minimum value allowed for this property state.
     * @param upperBound     The absolute maximum value allowed for this property state.
     */
    protected BoundedPropertyListener(ConfigDescription description, DynamicPropertyTypeRegistry.TypeDefinition<T> typeDefinition, T defaultValue, T lowerBound, T upperBound) {
        super(description, typeDefinition, defaultValue);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;

        // Eagerly resolve the property to ensure boundary constraints are registered immediately.
        this.resolveProperty(true);
    }

    /**
     * Resolves the target property from the active configuration manager instance.
     * Overrides the standard resolution pipeline to inject the explicit boundary constraints
     * during the property generation phase.
     *
     * @param forceResolution Bypasses the initialization state check to force synchronization.
     */
    @Override
    protected synchronized void resolveProperty(boolean forceResolution) {
        if (!this.initialized || forceResolution) {
            ConfigManager manager = ConfigRegistry.getOrCreateManager(this.description.modId());
            this.cachedProperty = manager.getOrCreateDynamicProperty(this.description, this.typeDefinition, this.defaultValue, this.lowerBound, this.upperBound);
            this.cachedProperty.addListener(this);
            this.initialized = true;
        }
    }
}