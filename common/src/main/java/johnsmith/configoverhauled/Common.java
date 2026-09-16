package johnsmith.configoverhauled;

import johnsmith.configoverhauled.api.registry.ConfigRegistry;

public class Common {
    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigRegistry::saveAllSynchronously));
    }
}