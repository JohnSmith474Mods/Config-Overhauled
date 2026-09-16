package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.Color;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class ARGBColorPropertyListener extends BoundedPropertyListener<Integer> {
    protected ARGBColorPropertyListener(ConfigDescription description, Integer defaultValue) {
        super(description, DynamicPropertyTypeRegistry.ARGB_COLOR, defaultValue, Color.ARGB_LOWER_BOUND, Color.ARGB_UPPER_BOUND);
    }
}
