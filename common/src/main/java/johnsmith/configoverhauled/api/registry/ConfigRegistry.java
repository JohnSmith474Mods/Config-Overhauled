package johnsmith.configoverhauled.api.registry;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.factory.ConfigFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized registry for managing and retrieving active configuration managers across all dependent mods.
 * Utilizes the Java ServiceLoader to dynamically resolve platform-specific factory implementations.
 */
public class ConfigRegistry {
    private static final Map<String, ConfigManager> GLOBAL_MANAGER_LOOKUP = new ConcurrentHashMap<>();
    private static final ConfigFactory FACTORY;

    static {
        FACTORY = ServiceLoader.load(ConfigFactory.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No IConfigFactory implementation found. Check META-INF/services."));
    }

    /**
     * Binds an initialized configuration manager to the registry under its respective mod identifier.
     *
     * @param manager The configuration manager instance to register.
     */
    public static void registerManager(ConfigManager manager) {
        GLOBAL_MANAGER_LOOKUP.putIfAbsent(manager.modId(), manager);
    }

    /**
     * Retrieves the registered configuration manager for a specific mod identifier.
     *
     * @param modId The target mod identifier.
     * @return The associated configuration manager, or null if unassigned.
     */
    public static ConfigManager getManager(String modId) {
        return GLOBAL_MANAGER_LOOKUP.get(modId);
    }

    /**
     * Retrieves an existing configuration manager or structurally allocates a new instance via the resolved factory.
     * Implements thread-safe double-checked locking to prevent race conditions during concurrent initialization.
     *
     * @param modId The target mod identifier.
     * @return A valid configuration manager scoped to the provided identifier.
     */
    public static ConfigManager getOrCreateManager(String modId) {
        ConfigManager manager = GLOBAL_MANAGER_LOOKUP.get(modId);
        if (manager == null) {
            synchronized (GLOBAL_MANAGER_LOOKUP) {
                manager = GLOBAL_MANAGER_LOOKUP.get(modId);
                if (manager == null) {
                    manager = FACTORY.create(modId);
                }
            }
        }
        return manager;
    }

    /**
     * Instructs all registered configuration managers to execute synchronous disk-write operations.
     */
    public static void saveAllSynchronously() {
        for (ConfigManager manager : GLOBAL_MANAGER_LOOKUP.values()) {
            manager.save();
        }
    }

    /**
     * Retrieves all currently active configuration managers.
     *
     * @return A collection of registered configuration managers.
     */
    public static Collection<ConfigManager> getAllManagers() {
        return GLOBAL_MANAGER_LOOKUP.values();
    }

    /**
     * Retrieves a list of all mod identifiers actively utilizing the framework.
     *
     * @return A list of registered mod identifiers.
     */
    public static List<String> getDependentModIDs() {
        return GLOBAL_MANAGER_LOOKUP.keySet().stream().toList();
    }
}