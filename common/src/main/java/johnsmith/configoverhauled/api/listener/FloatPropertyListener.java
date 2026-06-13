package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class FloatPropertyListener extends BoundedPropertyListener<Float> {
    protected FloatPropertyListener(ConfigDescription description, Float defaultValue, Float lowerBound, Float upperBound) {
        super(description, DynamicPropertyTypeRegistry.FLOAT, defaultValue, lowerBound, upperBound);
    }

    protected FloatPropertyListener(ConfigDescription description, Float defaultValue) {
        super(description, DynamicPropertyTypeRegistry.FLOAT, defaultValue, -Float.MAX_VALUE, Float.MAX_VALUE);
    }
}
