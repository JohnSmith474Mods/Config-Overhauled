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

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class AbstractDataGenCommand {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static <S> CompletableFuture<Suggestions> suggestModIds(CommandContext<S> context, SuggestionsBuilder builder) {
        for (String modId : ConfigRegistry.getDependentModIDs()) {
            builder.suggest(modId);
        }
        return builder.buildFuture();
    }

    protected static Component executeDatagen(String modId, String subDirectory, String successPrefix, Function<ConfigManager, JsonObject> payloadGenerator) {
        ConfigManager manager = ConfigRegistry.getManager(modId);
        if (manager == null) return Component.literal("Unrecognized mod ID: " + modId).withStyle(ChatFormatting.RED);

        JsonObject payload = payloadGenerator.apply(manager);

        try {
            Path rootDir = manager.getConfigDirectory().get();
            Path targetDir = rootDir.resolve(modId).resolve(subDirectory);
            Files.createDirectories(targetDir);

            String fileName = String.format("%s-%s.json", modId, LocalDateTime.now().format(FORMATTER));
            Path outPath = targetDir.resolve(fileName).toAbsolutePath();

            try (FileWriter writer = new FileWriter(outPath.toFile())) {
                GSON.toJson(payload, writer);
            }

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