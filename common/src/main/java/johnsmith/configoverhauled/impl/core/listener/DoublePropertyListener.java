package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;

public class DoublePropertyListener extends BoundedPropertyListener<Double> {
    protected DoublePropertyListener(ConfigDescription description, Double defaultValue, Double lowerBound, Double upperBound) {
        super(description, Double.class, defaultValue, lowerBound, upperBound);
    }

    protected DoublePropertyListener(ConfigDescription description, Double defaultValue) {
        super(description, Double.class, defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
    }
}
