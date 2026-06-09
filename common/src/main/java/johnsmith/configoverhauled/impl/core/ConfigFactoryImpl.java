package johnsmith.configoverhauled.impl.core;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.factory.ConfigFactory;
import org.slf4j.LoggerFactory;

public class ConfigFactoryImpl implements ConfigFactory {
    @Override
    public ConfigManager create(String modId) {
        return new ConfigManagerImpl(modId, LoggerFactory.getLogger(modId));
    }
}