package johnsmith.configoverhauled.impl.network.client;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;

import net.minecraft.nbt.CompoundTag;

public class ClientPayloadHandler {
    public static void handleSyncPacket(ConfigSyncPacket packet) {
        ConfigManager manager = ConfigRegistry.getManager(packet.modId());
        if (manager == null) return;

        CompoundTag payload = packet.configData();
        for (Category category : manager.getCategories()) {
            for (Group group : manager.getGroupsIn(category)) {
                for (Property<?> property : manager.getPropertiesIn(group)) {
                    if (payload.contains(property.getUniqueId())) {
                        property.decodeValueFromNbt(payload.get(property.getUniqueId()));
                    }
                }
            }
        }
    }
}
