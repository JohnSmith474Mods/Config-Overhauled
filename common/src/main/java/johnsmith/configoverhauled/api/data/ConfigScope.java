package johnsmith.configoverhauled.api.data;

/**
 * Defines the serialization boundaries, disk storage location, and network synchronization rules for a configuration property.
 */
public enum ConfigScope {
    /**
     * Operates exclusively within the local client environment.
     * Serialized to the root config directory. Rejects authoritative updates from active server connections.
     */
    CLIENT,

    /**
     * Operates universally across the environment instance.
     * Serialized to the root config directory. Applies to all local single-player levels or globally dictates behavior on a dedicated server.
     */
    GLOBAL,

    /**
     * Operates strictly within the boundary of a specific world instance.
     * Serialized to the level save directory. Server acts as the authoritative state and strictly synchronizes properties to all connected clients.
     */
    LEVEL
}