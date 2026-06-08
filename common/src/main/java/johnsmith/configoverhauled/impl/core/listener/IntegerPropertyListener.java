package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;

public class IntegerPropertyListener extends BoundedPropertyListener<Integer> {
    protected IntegerPropertyListener(ConfigDescription description, Integer defaultValue, Integer lowerBound, Integer upperBound) {
        super(description, Integer.class, defaultValue, lowerBound, upperBound);
    }
    protected IntegerPropertyListener(ConfigDescription description, Integer defaultValue) {
        super(description, Integer.class, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
