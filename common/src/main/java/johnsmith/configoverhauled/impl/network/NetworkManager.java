package johnsmith.configoverhauled.impl.network;

import johnsmith.configoverhauled.impl.platform.Services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class NetworkManager {
    public static void sendToClient(CustomPacketPayload payload, ServerPlayer player) {
        Services.PLATFORM.sendToClient(payload, player);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        Services.PLATFORM.sendToServer(payload);
    }

    public static void sendToAllClients(CustomPacketPayload payload, MinecraftServer server) {
        Services.PLATFORM.sendToAllClients(payload, server);
    }
}