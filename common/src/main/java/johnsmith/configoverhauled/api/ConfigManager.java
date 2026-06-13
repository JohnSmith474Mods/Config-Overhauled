package johnsmith.configoverhauled.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.data.ConfigScope;

import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;
import net.minecraft.server.MinecraftServer;

import org.slf4j.Logger;

/**
 * The authoritative controller for a mod's configuration state.
 * Manages disk I/O, hierarchical registry, and lifecycle synchronization across scopes.
 */
public interface ConfigManager {
    /**
     * Retrieves the bound namespace identifier.
     *
     * @return The registered mod ID.
     */
    String modId();

    /**
     * Retrieves the delegated logging framework instance.
     *
     * @return The SLF4J logger.
     */
    Logger logger();

    /**
     * Allocates primary disk paths and executes initial parsing for CLIENT and GLOBAL scopes.
     *
     * @param configDirectory The root configuration path provided by the platform environment.
     */
    void init(Path configDirectory);

    /**
     * Allocates dynamic disk paths and executes parsing for the LEVEL scope tied to the active world instance.
     *
     * @param server The active Minecraft server instance.
     */
    void attachServerConfig(MinecraftServer server);

    /**
     * Forces a synchronous disk read operation for all properties bound to the specified scope.
     *
     * @param targetScope The target synchronization scope.
     */
    void loadScope(ConfigScope targetScope);

    /**
     * Forces a synchronous disk write operation for all properties bound to the specified scope.
     *
     * @param targetScope The target synchronization scope.
     */
    void saveScope(ConfigScope targetScope);

    /**
     * Executes a synchronous disk read across all configuration scopes.
     */
    void load();

    /**
     * Executes a synchronous disk write across all configuration scopes.
     */
    void save();

    /**
     * Dispatches an asynchronous disk write operation across all configuration scopes.
     *
     * @return A CompletableFuture representing the state of the I/O execution.
     */
    CompletableFuture<Void> saveAll();

    void logInfo(String message, Object... params);
    void logWarn(String message, Object... params);
    void logError(String message, Object... params);
    void logDebug(String message, Object... params);

    /**
     * Resolves an existing category or constructs a new structural boundary.
     *
     * @param id The category identifier.
     * @return The bound Category instance.
     */
    Category define(String id);

    /**
     * Resolves an existing group or constructs a new structural entity within the specified category.
     *
     * @param category The parent structural boundary.
     * @param id       The group identifier.
     * @return The bound Group instance.
     */
    Group registerGroup(Category category, String id);

    /**
     * Injects a property into the active configuration memory state and synchronizes it with disk definitions.
     *
     * @param config The structural property definition.
     */
    <T> void registerProperty(Property<T> config);

    /**
     * Allocates a dynamic property instance at runtime, matching specific coordinate definitions.
     * Utilized primarily for datapack injection or authoritative server synchronization.
     *
     * @param description    The absolute coordinate map.
     * @param typeDefinition The underlying data class.
     * @param defaultValue   The baseline state value.
     * @param min            The lower validation boundary.
     * @param max            The upper validation boundary.
     * @return The dynamically constructed Property instance.
     */
    Property<?> getOrCreateDynamicProperty(ConfigDescription description, DynamicPropertyTypeRegistry.TypeDefinition<?> typeDefinition, Object defaultValue, Object min, Object max);

    /**
     * Executes hierarchical teardown and unbinds a target category and all dependencies.
     */
    void remove(Category category);

    /**
     * Executes hierarchical teardown and unbinds a target group and all dependencies.
     */
    void remove(Group group);

    /**
     * Detaches a specific property from the active memory state.
     */
    void remove(Property<?> property);

    /**
     * Retrieves all structurally active categories.
     */
    List<Category> getCategories();

    /**
     * Retrieves all structural groups bound to the specific category.
     */
    List<Group> getGroupsIn(Category category);

    /**
     * Retrieves all properties actively bound to the specified group.
     */
    List<Property<?>> getPropertiesIn(Group group);

    /**
     * Retrieves the target disk directory assigned during initialization.
     */
    Optional<Path> getConfigDirectory();

    /**
     * Traverses the active structural hierarchy to resolve a property matching exact logical coordinates.
     *
     * @param description The exact definition map.
     * @return The resolved Property instance, or null if unbound.
     */
    Property<?> findProperty(ConfigDescription description);

    /**
     * Injects a custom screen factory to override default interface rendering protocols.
     * Execute binding prior to platform-specific GUI endpoint registration.
     *
     * @param factory The functional screen construction sequence.
     */
    void setScreenFactory(java.util.function.Function<Object, Object> factory);

    /**
     * Triggers dynamic GUI generation for the active configuration hierarchy.
     *
     * @param parent The preceding menu screen entity.
     * @return The constructed configuration screen entity.
     */
    <S> S createScreen(S parent);

    /**
     * Injects a specialized widget resolution map, overriding the default implementation.
     */
    void setWidgetMapper(WidgetRegistry mapper);

    /**
     * Resolves the active widget mapper bound to this specific manager instance.
     */
    WidgetRegistry getWidgetMapper();
}