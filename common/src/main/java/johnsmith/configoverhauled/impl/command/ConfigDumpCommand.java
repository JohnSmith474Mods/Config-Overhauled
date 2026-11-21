package johnsmith.configoverhauled.impl.command;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.JavaOps;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ConfigDumpCommand extends AbstractDataGenCommand {
    public static final String FORMAT_TOML = "toml";
    public static final String FORMAT_JSON = "json";
    private static final List<String> SUPPORTED_FORMATS = List.of(FORMAT_TOML, FORMAT_JSON);
    private static final List<ConfigScope> SUPPORTED_SCOPES = List.of(ConfigScope.GLOBAL, ConfigScope.LEVEL);

    public static <S> CompletableFuture<Suggestions> suggestFormats(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (format.startsWith(remaining)) {
                builder.suggest(format);
            }
        }
        return builder.buildFuture();
    }

    public static <S> CompletableFuture<Suggestions> suggestScopes(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (ConfigScope scope : SUPPORTED_SCOPES) {
            if (scope.name().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(scope.name());
            }
        }
        return builder.buildFuture();
    }

    public static Component executeDump(String modId, String format, String scopeName) {
        String normalizedFormat = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
        if (!FORMAT_TOML.equals(normalizedFormat) && !FORMAT_JSON.equals(normalizedFormat)) {
            return Component.literal("Unsupported dump format: '" + format + "'. Expected 'toml' or 'json'.")
                    .withStyle(ChatFormatting.RED);
        }

        ConfigScope targetScope = parseSupportedScope(scopeName);
        if (targetScope == null) {
            return Component.literal("Unsupported dump scope: '" + scopeName + "'. Expected 'GLOBAL' or 'LEVEL'.")
                    .withStyle(ChatFormatting.RED);
        }

        ConfigManager manager = ConfigRegistry.getManager(modId);
        if (manager == null) {
            return Component.literal("Unrecognized mod ID: " + modId).withStyle(ChatFormatting.RED);
        }

        if (!hasPropertiesInScope(manager, targetScope)) {
            return Component.literal("No properties found in scope " + targetScope.name() + " for mod ID: " + modId)
                    .withStyle(ChatFormatting.YELLOW);
        }

        String filePrefix = modId + "-" + targetScope.name().toLowerCase(Locale.ROOT);

        return switch (normalizedFormat) {
            case FORMAT_TOML -> executeDatagenFile(modId, "dump", filePrefix, FORMAT_TOML, "Config Dump saved: ",
                    (mgr, outPath) -> writeTomlFile(mgr, targetScope, outPath));
            case FORMAT_JSON -> executeDatagenFile(modId, "dump", filePrefix, FORMAT_JSON, "Config Dump saved: ",
                    (mgr, outPath) -> Files.writeString(outPath, GSON.toJson(serializeJson(modId, mgr, targetScope)), StandardCharsets.UTF_8));
            default -> Component.literal("Unsupported dump format: '" + format + "'. Expected 'toml' or 'json'.")
                    .withStyle(ChatFormatting.RED);
        };
    }

    private static ConfigScope parseSupportedScope(String scopeName) {
        if (scopeName == null) {
            return null;
        }
        String normalized = scopeName.trim().toUpperCase(Locale.ROOT);
        for (ConfigScope scope : SUPPORTED_SCOPES) {
            if (scope.name().equals(normalized)) {
                return scope;
            }
        }
        return null;
    }

    private static boolean hasPropertiesInScope(ConfigManager manager, ConfigScope targetScope) {
        for (Category category : manager.getCategories()) {
            List<Group> groups = manager.getGroupsIn(category);
            if (groups == null) continue;
            for (Group group : groups) {
                List<Property<?>> properties = manager.getPropertiesIn(group);
                if (properties == null) continue;
                for (Property<?> property : properties) {
                    if (property.scope() == targetScope) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void writeTomlFile(ConfigManager manager, ConfigScope targetScope, Path outPath) {
        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(outPath).build()) {
            for (Category category : manager.getCategories()) {
                List<Group> groups = manager.getGroupsIn(category);
                if (groups == null) continue;

                for (Group group : groups) {
                    List<Property<?>> properties = manager.getPropertiesIn(group);
                    if (properties == null) continue;

                    for (Property<?> property : properties) {
                        if (property.scope() != targetScope) continue;
                        writePropertyToToml(property, fileConfig);
                    }
                }
            }
            fileConfig.save();
        }
    }

    private static <T> void writePropertyToToml(Property<T> property, CommentedConfig config) {
        property.save(config);
        String path = property.getUniqueId();
        String comment = config.getComment(path);
        property.codec().encodeStart(JavaOps.INSTANCE, property.get())
                .result()
                .ifPresent(serialized -> {
                    config.set(path, serialized);
                    if (comment != null) {
                        config.setComment(path, comment);
                    }
                });
    }

    private static JsonObject serializeJson(String modId, ConfigManager manager, ConfigScope targetScope) {
        JsonObject modObject = new JsonObject();
        JsonArray scopeArray = new JsonArray();

        for (Category category : manager.getCategories()) {
            List<Group> groups = manager.getGroupsIn(category);
            if (groups == null) continue;

            for (Group group : groups) {
                List<Property<?>> properties = manager.getPropertiesIn(group);
                if (properties == null) continue;

                for (Property<?> property : properties) {
                    if (property.scope() != targetScope) continue;

                    JsonObject propObject = new JsonObject();

                    JsonObject configObject = new JsonObject();
                    configObject.addProperty("mod_id", modId);
                    configObject.addProperty("category", category.id());
                    configObject.addProperty("group", group.id());
                    configObject.addProperty("property", property.resourceName());

                    propObject.add("config", configObject);
                    addValue(propObject, "value", property.get());
                    addValue(propObject, "default_value", property.defaultValue());

                    if (property instanceof DefaultProperty.Bounded<?> bounded) {
                        addValue(propObject, "min", bounded.lowerBound);
                        addValue(propObject, "max", bounded.upperBound);
                    }

                    scopeArray.add(propObject);
                }
            }
        }

        if (!scopeArray.isEmpty()) {
            modObject.add(targetScope.name(), scopeArray);
        }
        return modObject;
    }

    private static void addValue(JsonObject object, String key, Object value) {
        if (value instanceof Number n) object.addProperty(key, n);
        else if (value instanceof Boolean b) object.addProperty(key, b);
        else object.addProperty(key, String.valueOf(value));
    }
}