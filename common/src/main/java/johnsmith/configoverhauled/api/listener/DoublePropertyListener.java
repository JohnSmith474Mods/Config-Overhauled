package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class DoublePropertyListener extends BoundedPropertyListener<Double> {
    protected DoublePropertyListener(ConfigDescription description, Double defaultValue, Double lowerBound, Double upperBound) {
        super(description, DynamicPropertyTypeRegistry.DOUBLE, defaultValue, lowerBound, upperBound);
    }

    protected DoublePropertyListener(ConfigDescription description, Double defaultValue) {
        super(description, DynamicPropertyTypeRegistry.DOUBLE, defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
    }
}
