package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.data.Color;
import johnsmith.configoverhauled.api.data.ConfigDescription;

public class ColorPropertyListener extends BoundedPropertyListener<Integer> {
    protected ColorPropertyListener(ConfigDescription description, Integer defaultValue, Integer lowerBound, Integer upperBound) {
        super(description, Color.class, defaultValue, lowerBound, upperBound);
    }

    protected ColorPropertyListener(ConfigDescription description, Integer defaultValue) {
        super(description, Color.class, defaultValue, Color.LOWER_BOUND, Color.UPPER_BOUND);
    }
}
