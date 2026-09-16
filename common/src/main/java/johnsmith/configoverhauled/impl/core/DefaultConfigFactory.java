package johnsmith.configoverhauled.impl.core;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.factory.ConfigFactory;
import org.slf4j.LoggerFactory;

public class DefaultConfigFactory implements ConfigFactory {
    @Override
    public ConfigManager create(String modId) {
        return new DefaultConfigManager(modId, LoggerFactory.getLogger(modId));
    }
}