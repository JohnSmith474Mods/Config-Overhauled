package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;

public class LongPropertyListener extends BoundedPropertyListener<Long> {
    protected LongPropertyListener(ConfigDescription description, Long defaultValue, Long lowerBound, Long upperBound) {
        super(description, Long.class, defaultValue, lowerBound, upperBound);
    }

    protected LongPropertyListener(ConfigDescription description, Long defaultValue) {
        super(description, Long.class, defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
    }
}
