package johnsmith.configoverhauled.impl.network.client;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

public class ClientEventHandler {
    public static void onClientDisconnect() {
        for (ConfigManager manager : ConfigRegistry.getAllManagers()) {
            for (Category category : manager.getCategories()) {
                for (Group group : manager.getGroupsIn(category)) {
                    for (Property<?> property : manager.getPropertiesIn(group)) {
                        property.onDisconnect();
                    }
                }
            }
        }
    }

    public static void onClientStopping() {
        ConfigRegistry.saveAllSynchronously();
    }
}