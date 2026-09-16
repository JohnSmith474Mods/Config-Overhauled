package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class BooleanPropertyListener extends AbstractPropertyListener<Boolean> {
    protected BooleanPropertyListener(ConfigDescription description, Boolean defaultValue) {
        super(description, DynamicPropertyTypeRegistry.BOOLEAN, defaultValue);
        this.resolveProperty(true);
    }
}
