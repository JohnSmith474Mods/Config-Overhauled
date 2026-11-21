package johnsmith.configoverhauled.impl.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import johnsmith.configoverhauled.Config;
import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class AbstractDataGenCommand {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static <S> CompletableFuture<Suggestions> suggestModIds(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (String modId : ConfigRegistry.getDependentModIDs()) {
            if (modId.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(modId);
            }
        }
        return builder.buildFuture();
    }

    @FunctionalInterface
    protected interface FileWriterCallback {
        void write(ConfigManager manager, Path outPath) throws Exception;
    }

    protected static Component executeDatagen(String modId, String subDirectory, String successPrefix, Function<ConfigManager, JsonObject> payloadGenerator) {
        return executeDatagen(modId, subDirectory, "json", successPrefix, manager -> GSON.toJson(payloadGenerator.apply(manager)));
    }

    protected static Component executeDatagen(String modId, String subDirectory, String extension, String successPrefix, Function<ConfigManager, String> contentGenerator) {
        return executeDatagenFile(modId, subDirectory, modId, extension, successPrefix, (manager, outPath) -> {
            String content = contentGenerator.apply(manager);
            Files.writeString(outPath, content, StandardCharsets.UTF_8);
        });
    }

    protected static Component executeDatagenFile(
            String modId,
            String subDirectory,
            String fileNamePrefix,
            String extension,
            String successPrefix,
            FileWriterCallback writerCallback
    ) {
        ConfigManager manager = ConfigRegistry.getManager(modId);
        if (manager == null) return Component.literal("Unrecognized mod ID: " + modId).withStyle(ChatFormatting.RED);

        Optional<Path> configDirOpt = manager.getConfigDirectory();
        if (configDirOpt.isEmpty()) {
            return Component.literal("Configuration directory is not initialized for mod ID: " + modId).withStyle(ChatFormatting.RED);
        }

        try {
            Path rootDir = configDirOpt.get();
            Path targetDir = rootDir.resolve(modId).resolve(subDirectory);
            Files.createDirectories(targetDir);

            String normalizedExt = extension.startsWith(".") ? extension.substring(1) : extension;
            String fileName = String.format("%s-%s.%s", fileNamePrefix, LocalDateTime.now().format(FORMATTER), normalizedExt);
            Path outPath = targetDir.resolve(fileName).toAbsolutePath();

            writerCallback.write(manager, outPath);

            Component pathComponent = Component.literal(fileName)
                    .withStyle(style -> style.withUnderlined(true)
                            .withColor(TextColor.fromRgb(Config.DATA_GEN_LINK_COLOR.get()))
                            .withClickEvent(new ClickEvent.OpenFile(targetDir.toString()))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open folder"))));

            return Component.literal(successPrefix).append(pathComponent);
        } catch (Exception e) {
            Constants.LOG.error("Datagen failed for " + subDirectory, e);
            return Component.literal("Write operation failed.").withStyle(ChatFormatting.RED);
        }
    }
}