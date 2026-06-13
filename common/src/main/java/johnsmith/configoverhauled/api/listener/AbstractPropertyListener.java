package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

import org.jetbrains.annotations.Nullable;

/**
 * Abstract base implementation for dynamic configuration property listeners.
 * Facilitates lazy resolution and state synchronization of dynamically generated properties.
 *
 * @param <T> The underlying data type monitored by this listener.
 */
public abstract class AbstractPropertyListener<T> implements Property.Listener {

    /** The actively tracked property instance. Null prior to initialization or post-invalidation. */
    @Nullable
    protected Property<?> cachedProperty = null;

    /** Tracks the resolution state of the listener instance. */
    protected volatile boolean initialized = false;

    /** The structural definition dictating the property's data type and serialization logic. */
    protected final DynamicPropertyTypeRegistry.TypeDefinition<T> typeDefinition;

    /** The metadata encapsulating the property's identity and organizational hierarchy. */
    protected final ConfigDescription description;

    /** The fallback value utilized during property generation. */
    protected final T defaultValue;

    /**
     * Constructs a state listener for a dynamic configuration property.
     *
     * @param description    The structural and organizational metadata for the target property.
     * @param typeDefinition The registry definition mapping the required data type.
     * @param defaultValue   The default value assigned upon initial property generation.
     */
    protected AbstractPropertyListener(ConfigDescription description, DynamicPropertyTypeRegistry.TypeDefinition<T> typeDefinition, T defaultValue) {
        this.typeDefinition = typeDefinition;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * Executes upon the destruction or desynchronization of the bound property instance.
     * Purges the internal cache and resets the initialization state.
     */
    @Override
    public void onPropertyInvalidated() {
        this.initialized = false;
        this.cachedProperty = null;
    }

    /**
     * Executes upon a state mutation within the bound property.
     * Forces a re-evaluation of the property resolution pipeline.
     */
    @Override
    public void onPropertyChanged() {
        this.resolveProperty(true);
    }

    /**
     * Resolves the target property from the active configuration manager instance.
     * Lazily binds this listener to the target property to monitor future state mutations.
     *
     * @param forceResolution Bypasses the initialization state check to force synchronization.
     */
    protected synchronized void resolveProperty(boolean forceResolution) {
        if (!this.initialized || forceResolution) {
            ConfigManager manager = ConfigRegistry.getOrCreateManager(this.description.modId());
            this.cachedProperty = manager.getOrCreateDynamicProperty(this.description, this.typeDefinition, this.defaultValue, this.defaultValue, this.defaultValue);
            this.cachedProperty.addListener(this);
            this.initialized = true;
        }
    }
}