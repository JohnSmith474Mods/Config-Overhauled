package johnsmith.configoverhauled.api.factory;

import johnsmith.configoverhauled.api.ConfigManager;

/**
 * Service provider interface for initializing the platform-specific configuration environment.
 * Implementations are resolved dynamically via the Java ServiceLoader mechanism.
 */
public interface ConfigFactory {
    /**
     * Constructs and binds a new configuration manager instance to the specified namespace.
     *
     * @param modId The unique identifier of the target mod utilizing the framework.
     * @return A fully initialized configuration manager scoped to the provided identifier.
     */
    ConfigManager create(String modId);
}