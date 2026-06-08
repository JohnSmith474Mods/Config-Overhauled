package johnsmith.configoverhauled.impl.core.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;

public class BooleanPropertyListener extends AbstractPropertyListener<Boolean> {
    protected BooleanPropertyListener(ConfigDescription description, Boolean defaultValue) {
        super(description, Boolean.class, defaultValue);
        this.resolveProperty(true);
    }
}
