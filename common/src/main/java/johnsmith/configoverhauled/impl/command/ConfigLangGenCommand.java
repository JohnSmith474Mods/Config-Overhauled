package johnsmith.configoverhauled.impl.command;

import com.google.gson.JsonObject;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ConfigLangGenCommand extends AbstractDataGenCommand {

    public static Component executeLang(String modId) {
        return executeLang(modId, true);
    }

    public static Component executeLang(String modId, boolean onlyMissingEntries) {
        ConfigManager manager = ConfigRegistry.getManager(modId);
        if (manager == null) {
            return Component.literal("Unrecognized mod ID: " + modId).withStyle(ChatFormatting.RED);
        }

        JsonObject langObject = buildLangObject(manager, onlyMissingEntries);
        if (onlyMissingEntries && langObject.isEmpty()) {
            return Component.literal("No missing localization entries found for " + modId + ".")
                    .withStyle(ChatFormatting.GREEN);
        }

        return executeDatagen(modId, "lang", "Lang file generated: ", ignored -> langObject);
    }

    private static JsonObject buildLangObject(ConfigManager manager, boolean onlyMissingEntries) {
        JsonObject langObject = new JsonObject();
        Language language = Language.getInstance();

        for (Category category : manager.getCategories()) {
            addIfNeeded(langObject, category.translationKey(), onlyMissingEntries, language);

            List<Group> groups = manager.getGroupsIn(category);
            if (groups == null) continue;

            for (Group group : groups) {
                addIfNeeded(langObject, group.translationKey(), onlyMissingEntries, language);

                List<Property<?>> properties = manager.getPropertiesIn(group);
                if (properties == null) continue;

                for (Property<?> property : properties) {
                    addIfNeeded(langObject, property.translationKey(), onlyMissingEntries, language);
                    addIfNeeded(langObject, property.descriptionTranslationKey(), onlyMissingEntries, language);
                }
            }
        }
        return langObject;
    }

    private static void addIfNeeded(JsonObject langObject, String key, boolean onlyMissingEntries, Language language) {
        if (!onlyMissingEntries || !language.has(key)) {
            langObject.addProperty(key, "");
        }
    }
}