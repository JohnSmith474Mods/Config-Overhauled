package johnsmith.configoverhauled.impl.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.impl.core.state.PropertyImpl;
import net.minecraft.network.chat.Component;

public class ConfigDumpCommand extends AbstractDataGenCommand {

    public static Component executeDump(String modId) {
        return executeDatagen(modId, "dump", "Config Dump saved: ", manager -> {
            JsonObject modObject = new JsonObject();
            for (ConfigScope scope : ConfigScope.values()) {
                JsonArray scopeArray = new JsonArray();
                for (Category category : manager.getCategories()) {
                    for (Group group : manager.getGroupsIn(category)) {
                        for (Property<?> property : manager.getPropertiesIn(group)) {
                            if (property.scope() != scope) continue;

                            JsonObject propObject = new JsonObject();

                            JsonObject configObject = new JsonObject();
                            configObject.addProperty("mod_id", modId);
                            configObject.addProperty("category", category.id());
                            configObject.addProperty("group", group.id());
                            configObject.addProperty("property", property.resourceName());

                            propObject.add("config", configObject);
                            addValue(propObject, "value", property.get());
                            addValue(propObject, "default_value", property.defaultValue());

                            if (property instanceof PropertyImpl.Bounded<?> bounded) {
                                addValue(propObject, "min", bounded.lowerBound);
                                addValue(propObject, "max", bounded.upperBound);
                            }

                            scopeArray.add(propObject);
                        }
                    }
                }
                if (!scopeArray.isEmpty()) modObject.add(scope.name(), scopeArray);
            }
            return modObject;
        });
    }

    private static void addValue(JsonObject object, String key, Object value) {
        if (value instanceof Number n) object.addProperty(key, n);
        else if (value instanceof Boolean b) object.addProperty(key, b);
        else object.addProperty(key, String.valueOf(value));
    }
}