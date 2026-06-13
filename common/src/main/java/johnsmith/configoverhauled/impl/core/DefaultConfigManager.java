package johnsmith.configoverhauled.impl.core;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.nio.file.Path;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.factory.PropertyFactory;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;
import johnsmith.configoverhauled.impl.client.gui.registry.DefaultWidgetRegistry;
import johnsmith.configoverhauled.impl.client.gui.screen.ConfigScreenImpl;
import johnsmith.configoverhauled.impl.core.state.DefaultCategory;
import johnsmith.configoverhauled.impl.core.state.DefaultGroup;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import org.slf4j.Logger;

public class DefaultConfigManager implements ConfigManager {
    private final String modId;
    private final Logger logger;

    private Path configDirectory;
    private CommentedFileConfig clientConfig;
    private CommentedFileConfig globalConfig;
    private CommentedFileConfig levelConfig;

    private final Map<String, Category> categories = new ConcurrentHashMap<>();
    private final Map<Category, List<Group>> categoriesToGroups = new ConcurrentHashMap<>();
    private final Map<Group, List<Property<?>>> groupsToProperties = new ConcurrentHashMap<>();

    private Function<Object, Object> screenFactory;
    private WidgetRegistry widgetMapper;

    public DefaultConfigManager(String modId, Logger logger) {
        if (logger == null) throw new IllegalArgumentException("Logger cannot be null");
        this.logger = logger;

        if (modId == null || modId.isEmpty()) {
            logError("Mod ID cannot be null or empty.");
            throw new IllegalArgumentException("Mod ID cannot be null or empty");
        }
        this.modId = modId;
        this.widgetMapper = new DefaultWidgetRegistry();

        ConfigRegistry.registerManager(this);
        logInfo("Initialized ConfigManagerImpl.");
    }

    @Override public String modId() {
        logDebug("Fetching modId: {}", this.modId);
        return this.modId;
    }

    @Override public Logger logger() {
        return this.logger;
    }

    @Override
    public void init(Path configDirectory) {
        logInfo("Initializing configuration directory: {}", configDirectory);
        this.configDirectory = configDirectory;
        Path modConfigDir = configDirectory.resolve(this.modId);

        try {
            java.nio.file.Files.createDirectories(modConfigDir);
        } catch (java.io.IOException e) {
            logError("Failed to create config directory for {}: {}", modId, e.getMessage());
        }

        Path clientPath = modConfigDir.resolve(this.modId + "-client.toml");
        Path globalPath = modConfigDir.resolve(this.modId + "-global.toml");

        this.clientConfig = CommentedFileConfig.builder(clientPath).sync().autosave().build();
        this.globalConfig = CommentedFileConfig.builder(globalPath).sync().autosave().build();

        this.clientConfig.load();
        this.globalConfig.load();

        loadScope(ConfigScope.CLIENT);
        loadScope(ConfigScope.GLOBAL);
        saveScope(ConfigScope.CLIENT);
        saveScope(ConfigScope.GLOBAL);
        logInfo("Initialization complete.");
    }

    @Override
    public void attachServerConfig(MinecraftServer server) {
        logInfo("Attaching server configuration.");
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path modServerConfigDir = worldDir.resolve("serverconfig").resolve(this.modId);

        try {
            java.nio.file.Files.createDirectories(modServerConfigDir);
        } catch (java.io.IOException e) {
            logError("Failed to create server config directory for {}: {}", modId, e.getMessage());
        }

        Path levelPath = modServerConfigDir.resolve(this.modId + "-level.toml");
        this.levelConfig = CommentedFileConfig.builder(levelPath).sync().autosave().build();
        this.levelConfig.load();

        loadScope(ConfigScope.LEVEL);
        saveScope(ConfigScope.LEVEL);
        logInfo("Server configuration attached to: {}", levelPath);
    }

    private CommentedFileConfig getConfigForScope(ConfigScope scope) {
        logDebug("Retrieving CommentedFileConfig for scope: {}", scope);
        return switch (scope) {
            case CLIENT -> this.clientConfig;
            case GLOBAL -> this.globalConfig;
            case LEVEL -> this.levelConfig;
        };
    }

    @Override
    public void loadScope(ConfigScope targetScope) {
        logInfo("Loading configuration scope: {}", targetScope);
        CommentedFileConfig targetFile = getConfigForScope(targetScope);
        if (targetFile == null) return;

        synchronized (targetFile) {
            for (Category category : getCategories()) {
                List<Group> groups = getGroupsIn(category);
                if (groups == null) continue;
                for (Group group : groups) {
                    List<Property<?>> properties = getPropertiesIn(group);
                    if (properties == null) continue;
                    for (Property<?> property : properties) {
                        if (property.scope() == targetScope) property.load(targetFile);
                    }
                }
            }
        }
    }

    @Override
    public void saveScope(ConfigScope targetScope) {
        logInfo("Saving configuration scope: {}", targetScope);
        CommentedFileConfig targetFile = getConfigForScope(targetScope);
        if (targetFile == null) return;

        synchronized (targetFile) {
            for (Category category : getCategories()) {
                List<Group> groups = getGroupsIn(category);
                if (groups == null) continue;
                for (Group group : groups) {
                    List<Property<?>> properties = getPropertiesIn(group);
                    if (properties == null) continue;
                    for (Property<?> property : properties) {
                        if (property.scope() == targetScope) property.save(targetFile);
                    }
                }
            }
            targetFile.save();
        }
    }

    @Override public void load() {
        logInfo("Loading all configuration scopes.");
        loadScope(ConfigScope.CLIENT);
        loadScope(ConfigScope.GLOBAL);
        loadScope(ConfigScope.LEVEL);
    }

    @Override public void save() {
        logInfo("Saving all configuration scopes.");
        saveScope(ConfigScope.CLIENT);
        saveScope(ConfigScope.GLOBAL);
        saveScope(ConfigScope.LEVEL);
    }

    @Override public CompletableFuture<Void> saveAll() {
        logInfo("Dispatching asynchronous save operation for all scopes.");
        return CompletableFuture.runAsync(this::save);
    }

    @Override public void logInfo(String message, Object... params) { logger.info(message, params); }
    @Override public void logWarn(String message, Object... params) { logger.warn(message, params); }
    @Override public void logError(String message, Object... params) { logger.error(message, params); }
    @Override public void logDebug(String message, Object... params) { logger.debug(message, params); }

    @Override
    public Category define(String id) {
        if (id == null) {
            logError("Cannot register category: ID is null.");
            throw new IllegalArgumentException("Category ID cannot be null");
        }
        return categories.computeIfAbsent(id, k -> {
            logDebug("Registering new category: {}", id);
            Category category = new DefaultCategory(id, this);
            categoriesToGroups.put(category, new CopyOnWriteArrayList<>());
            return category;
        });
    }

    @Override
    public Group registerGroup(Category category, String id) {
        if (category == null || id == null) {
            logError("Cannot register group: Category or ID is null.");
            throw new IllegalArgumentException("Category and Group ID cannot be null");
        }
        List<Group> groups = categoriesToGroups.get(category);
        if (groups == null) {
            logError("Cannot register group '{}': Category '{}' is not registered to this manager.", id, category.id());
            throw new IllegalArgumentException("Category " + category.id() + " is not registered");
        }

        synchronized (groups) {
            return groups.stream()
                    .filter(g -> g.id().equals(id))
                    .findFirst()
                    .orElseGet(() -> {
                        logDebug("Registering new PropertyGroup: {} in Category: {}", id, category.id());
                        Group group = new DefaultGroup(category, id, this);
                        groups.add(group);
                        groupsToProperties.put(group, new CopyOnWriteArrayList<>());
                        return group;
                    });
        }
    }

    @Override
    public <T> void registerProperty(Property<T> config) {
        if (config == null) {
            logError("Cannot register config: property is null.");
            return;
        }
        Group group = config.parentGroup();
        List<Property<?>> properties = groupsToProperties.get(group);
        if (properties == null) {
            logError("Cannot register config '{}': Parent group is null or not registered.", config.getUniqueId());
            throw new IllegalArgumentException("Group is not registered");
        }

        synchronized (properties) {
            if (!properties.contains(config)) {
                logDebug("Registering config property: {}", config.getUniqueId());
                properties.add(config);
            }
        }

        CommentedFileConfig targetFile = getConfigForScope(config.scope());
        if (targetFile != null) {
            synchronized (targetFile) {
                config.load(targetFile);
            }
        }
    }

    @Override
    public Property<?> getOrCreateDynamicProperty(ConfigDescription description, DynamicPropertyTypeRegistry.TypeDefinition<?> typeDefinition, Object defaultValue, Object min, Object max) {
        logDebug("Retrieving or creating dynamic property for resource: {}", description.property());
        Category category = this.define(description.category());
        Group group = this.registerGroup(category, description.group());
        List<Property<?>> properties = this.groupsToProperties.get(group);

        synchronized (properties) {
            for (Property<?> existing : properties) {
                if (existing.resourceName().equals(description.property())) return existing;
            }
            PropertyFactory<?> factory = typeDefinition.factory();
            Property<?> dynamicProperty = factory.create(description.property(), group, defaultValue, min, max, typeDefinition.codec());
            this.registerProperty(dynamicProperty);
            return dynamicProperty;
        }
    }

    @Override
    public void remove(Category category) {
        logDebug("Removing category: {}", category.id());
        this.categories.remove(category.id());
        List<Group> groups = this.getGroupsIn(category);
        if (groups != null && !groups.isEmpty()) {
            synchronized (groups) {
                for (Group group : groups) this.remove(group);
            }
        }
        this.categoriesToGroups.remove(category);
    }

    @Override
    public void remove(Group group) {
        logDebug("Removing group: {} from category: {}", group.id(), group.parent().id());
        List<Group> siblingGroups = this.getGroupsIn(group.parent());
        if (siblingGroups != null) {
            synchronized (siblingGroups) {
                siblingGroups.remove(group);
            }
        }
        List<Property<?>> properties = this.getPropertiesIn(group);
        if (properties != null && !properties.isEmpty()) {
            synchronized (properties) {
                for (Property<?> property : properties) this.remove(property);
            }
        }
        this.groupsToProperties.remove(group);
    }

    @Override
    public void remove(Property<?> property) {
        logDebug("Removing property: {} from group: {}", property.resourceName(), property.parentGroup().id());
        List<Property<?>> properties = this.getPropertiesIn(property.parentGroup());
        if (properties != null) {
            synchronized (properties) {
                properties.remove(property);
            }
        }
    }

    @Override public List<Category> getCategories() {
        logDebug("Fetching all registered categories.");
        return categories.values().stream().toList();
    }

    @Override public List<Group> getGroupsIn(Category category) {
        logDebug("Fetching groups mapped to category: {}", category.id());
        return categoriesToGroups.get(category);
    }

    @Override public List<Property<?>> getPropertiesIn(Group group) {
        logDebug("Fetching properties mapped to group: {}", group.id());
        return groupsToProperties.get(group);
    }

    @Override public Optional<Path> getConfigDirectory() {
        logDebug("Fetching root configuration directory.");
        return Optional.ofNullable(this.configDirectory);
    }

    @Override
    public Property<?> findProperty(ConfigDescription description) {
        logDebug("Locating property matching criteria: {} / {} / {}", description.category(), description.group(), description.property());
        for (Category category : this.getCategories()) {
            if (!category.id().equals(description.category())) continue;
            for (Group group : this.getGroupsIn(category)) {
                if (!group.id().equals(description.group())) continue;
                for (Property<?> property : this.getPropertiesIn(group)) {
                    if (property.resourceName().equals(description.property())) return property;
                }
            }
        }
        return null;
    }

    @Override
    public void setScreenFactory(java.util.function.Function<Object, Object> factory) {
        this.screenFactory = factory;
        logInfo("Custom screen factory injected.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S createScreen(S parent) {
        if (this.screenFactory != null) {
            logDebug("Constructing GUI via injected custom screen factory.");
            return (S) this.screenFactory.apply(parent);
        }
        logDebug("Constructing default ConfigScreen instance.");
        return (S) new ConfigScreenImpl((Screen) parent, this);
    }

    @Override
    public void setWidgetMapper(WidgetRegistry mapper) {
        if (mapper == null) throw new IllegalArgumentException("Widget mapper cannot be null.");
        this.widgetMapper = mapper;
        logInfo("Custom widget mapper injected.");
    }

    @Override
    public WidgetRegistry getWidgetMapper() {
        return this.widgetMapper;
    }
}