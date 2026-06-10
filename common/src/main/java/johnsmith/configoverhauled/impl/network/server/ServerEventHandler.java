package johnsmith.configoverhauled.impl.network.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.impl.network.NetworkManager;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ServerEventHandler {
    public static void onPlayerJoin(ServerPlayer player) {
        CompletableFuture.runAsync(() -> {
            List<ConfigSyncPacket> packetsToSend = new ArrayList<>();

            for (ConfigManager manager : ConfigRegistry.getAllManagers()) {
                CompoundTag payload = new CompoundTag();
                boolean requiresSync = false;

                for (Category category : manager.getCategories()) {
                    for (Group group : manager.getGroupsIn(category)) {
                        for (Property<?> property : manager.getPropertiesIn(group)) {
                            if (property.scope() != ConfigScope.CLIENT) {
                                payload.put(property.getUniqueId(), property.encodeValueToNbt());
                                requiresSync = true;
                            }
                        }
                    }
                }

                if (requiresSync) {
                    packetsToSend.add(new ConfigSyncPacket(manager.modId(), payload));
                }
            }

            if (!packetsToSend.isEmpty()) {
                player.server.execute(() -> {
                    for (ConfigSyncPacket packet : packetsToSend) {
                        NetworkManager.sendToClient(packet, player);
                    }
                });
            }
        });
    }

    public static void onServerStarting(MinecraftServer server) {
        for (ConfigManager manager : ConfigRegistry.getAllManagers()) {
            manager.attachServerConfig(server);
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        ConfigRegistry.saveAllSynchronously();
    }
}