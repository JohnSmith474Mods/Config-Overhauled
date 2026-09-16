package johnsmith.configoverhauled;

import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigUpdateRequestPacket;
import johnsmith.configoverhauled.impl.network.server.ServerEventHandler;
import johnsmith.configoverhauled.impl.network.server.ServerPayloadHandler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class Fabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        Common.init();
        Config.MANAGER.init(FabricLoader.getInstance().getConfigDir());

        ServerLifecycleEvents.SERVER_STARTING.register(ServerEventHandler::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerEventHandler::onServerStopping);

        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPacket.TYPE, ConfigSyncPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConfigUpdateRequestPacket.TYPE, ConfigUpdateRequestPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdateRequestPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerPayloadHandler.handleUpdateRequestPacket(payload, context.player()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerEventHandler.onPlayerJoin(handler.player);
        });
    }
}
