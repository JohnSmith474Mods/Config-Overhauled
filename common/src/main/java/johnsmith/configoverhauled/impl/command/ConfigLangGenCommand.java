package johnsmith.configoverhauled.impl.command;

import com.google.gson.JsonObject;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;

import net.minecraft.network.chat.Component;

public class ConfigLangGenCommand extends AbstractDataGenCommand {

    public static Component executeLang(String modId) {
        return executeDatagen(modId, "lang", "Lang file generated: ", manager -> {
            JsonObject langObject = new JsonObject();

            for (Category category : manager.getCategories()) {
                String catKey = modId + ".config." + category.id();
                langObject.addProperty(catKey, "");

                for (Group group : manager.getGroupsIn(category)) {
                    String groupKey = catKey + "." + group.id();
                    langObject.addProperty(groupKey, "");

                    for (Property<?> property : manager.getPropertiesIn(group)) {
                        langObject.addProperty(property.translationKey(), "");
                        langObject.addProperty(property.descriptionTranslationKey(), "");
                    }
                }
            }
            return langObject;
        });
    }
}