package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class IntegerPropertyListener extends BoundedPropertyListener<Integer> {
    protected IntegerPropertyListener(ConfigDescription description, Integer defaultValue, Integer lowerBound, Integer upperBound) {
        super(description, DynamicPropertyTypeRegistry.INTEGER, defaultValue, lowerBound, upperBound);
    }
    protected IntegerPropertyListener(ConfigDescription description, Integer defaultValue) {
        super(description, DynamicPropertyTypeRegistry.INTEGER, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
