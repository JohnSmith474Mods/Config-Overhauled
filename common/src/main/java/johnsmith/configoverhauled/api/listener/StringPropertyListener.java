package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class StringPropertyListener extends AbstractPropertyListener<String> {
    protected StringPropertyListener(ConfigDescription description, String defaultValue) {
        super(description,  DynamicPropertyTypeRegistry.STRING, defaultValue);
        this.resolveProperty(true);
    }
}
