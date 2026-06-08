package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

public abstract class BoundedPropertyListener<T> extends AbstractPropertyListener<T> {
    protected final T lowerBound;
    protected final T upperBound;


    protected BoundedPropertyListener(ConfigDescription description, Class<?> configType, T defaultValue, T lowerBound, T upperBound) {
        super(description, configType, defaultValue);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.resolveProperty(true);
    }

    @Override
    protected synchronized void resolveProperty(boolean forceResolution) {
        if (!this.initialized || forceResolution) {
            ConfigManager manager = ConfigRegistry.getOrCreateManager(this.description.modId());
            this.cachedProperty = manager.getOrCreateDynamicProperty(this.description, this.configType, this.defaultValue, this.lowerBound, this.upperBound);
            this.cachedProperty.addListener(this);
            this.initialized = true;
        }
    }
}
