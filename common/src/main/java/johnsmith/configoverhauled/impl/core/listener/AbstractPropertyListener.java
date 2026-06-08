package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

import org.jetbrains.annotations.Nullable;

public abstract class AbstractPropertyListener<T> implements Property.Listener {
    @Nullable
    protected Property<?> cachedProperty = null;
    protected volatile boolean initialized = false;

    protected final Class<?> configType;
    protected final ConfigDescription description;
    protected final T defaultValue;

    protected AbstractPropertyListener(ConfigDescription description, Class<?> configType, T defaultValue) {
        this.configType = configType;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    @Override
    public void onPropertyInvalidated() {
        this.initialized = false;
        this.cachedProperty = null;
    }

    @Override
    public void onPropertyChanged() {
        this.resolveProperty(true);
    }

    protected synchronized void resolveProperty(boolean forceResolution) {
        if (!this.initialized || forceResolution) {
            ConfigManager manager = ConfigRegistry.getOrCreateManager(this.description.modId());
            this.cachedProperty = manager.getOrCreateDynamicProperty(this.description, this.configType, this.defaultValue, this.defaultValue, this.defaultValue);
            this.cachedProperty.addListener(this);
            this.initialized = true;
        }
    }
}
