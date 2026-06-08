package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;

public class FloatPropertyListener extends BoundedPropertyListener<Float> {
    protected FloatPropertyListener(ConfigDescription description, Float defaultValue, Float lowerBound, Float upperBound) {
        super(description, Float.class, defaultValue, lowerBound, upperBound);
    }

    protected FloatPropertyListener(ConfigDescription description, Float defaultValue) {
        super(description, Float.class, defaultValue, -Float.MAX_VALUE, Float.MAX_VALUE);
    }
}
