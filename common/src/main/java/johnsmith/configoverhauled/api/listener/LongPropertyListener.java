package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

public class LongPropertyListener extends BoundedPropertyListener<Long> {
    protected LongPropertyListener(ConfigDescription description, Long defaultValue, Long lowerBound, Long upperBound) {
        super(description, DynamicPropertyTypeRegistry.LONG, defaultValue, lowerBound, upperBound);
    }

    protected LongPropertyListener(ConfigDescription description, Long defaultValue) {
        super(description, DynamicPropertyTypeRegistry.LONG, defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
    }
}
