package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.Color;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class RGBColorPropertyListener extends BoundedPropertyListener<Integer> {
    protected RGBColorPropertyListener(ConfigDescription description, Integer defaultValue) {
        super(description, DynamicPropertyTypeRegistry.RGB_COLOR, defaultValue, Color.RGB_LOWER_BOUND, Color.RGB_UPPER_BOUND);
    }
}
