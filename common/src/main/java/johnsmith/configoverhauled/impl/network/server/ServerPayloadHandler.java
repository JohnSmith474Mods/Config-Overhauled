package johnsmith.configoverhauled.impl.network.server;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigUpdateRequestPacket;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;
import johnsmith.configoverhauled.impl.platform.Services;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

public class ServerPayloadHandler {
    public static void handleUpdateRequestPacket(ConfigUpdateRequestPacket packet, ServerPlayer sender) {
        // 0. Ignore unauthorized config edits.
        boolean isAuthorized = sender.hasPermissions(2) || sender.getServer().isSingleplayerOwner(sender.getGameProfile());
        if (!isAuthorized) {
            return;
        }

        // 1. Retrieve the ConfigManager from the ConfigRegistry.
        ConfigManager manager = ConfigRegistry.getManager(packet.metadata().modId());
        if (manager == null) return;

        // 2. Retrieve the Property from the ConfigManager.
        Property<?> property = manager.findProperty(packet.metadata());
        if (property == null || property.scope() == ConfigScope.CLIENT) return;

        // 4. Update the property.
        property.acceptAuthoritativeUpdate(packet.getEncodedValue());

        // 5. Save the new value to disk.
        manager.save();

        // 6. Issue a broadcast sync for this property only.
        ConfigSyncPacket broadcastPacket = createBroadcastPacket(manager, property);
        Services.PLATFORM.sendToAllClients(broadcastPacket, sender.getServer());
    }

    private static ConfigSyncPacket createBroadcastPacket(ConfigManager manager, Property<?> property) {
        CompoundTag syncPayload = new CompoundTag();
        syncPayload.put(property.getUniqueId(), property.encodeValueToNbt());
        return new ConfigSyncPacket(manager.modId(), syncPayload);
    }
}