package johnsmith.configoverhauled.network;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigUpdateRequestPacket;
import johnsmith.configoverhauled.impl.network.client.ClientPayloadHandler;
import johnsmith.configoverhauled.impl.network.server.ServerPayloadHandler;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoForgeNetworkRegistration {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToClient(
                ConfigSyncPacket.TYPE,
                ConfigSyncPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handleSyncPacket(payload))
        );

        registrar.playToServer(
                ConfigUpdateRequestPacket.TYPE,
                ConfigUpdateRequestPacket.STREAM_CODEC,
                (payload, context) -> {
                    ServerPlayer player = (ServerPlayer) context.player();
                    context.enqueueWork(() -> ServerPayloadHandler.handleUpdateRequestPacket(payload, player));
                }
        );
    }
}