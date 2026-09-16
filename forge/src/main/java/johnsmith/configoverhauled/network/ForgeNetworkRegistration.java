package johnsmith.configoverhauled.network;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.network.client.ClientPayloadHandler;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigUpdateRequestPacket;
import johnsmith.configoverhauled.impl.network.server.ServerPayloadHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

public class ForgeNetworkRegistration {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    public static void setup(final FMLCommonSetupEvent event) {
        int id = 0;

        CHANNEL.messageBuilder(ConfigSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((payload, buffer) -> ConfigSyncPacket.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> ConfigSyncPacket.STREAM_CODEC.decode(buffer))
                .consumerNetworkThread((payload, context) -> {
                    context.enqueueWork(() -> ClientPayloadHandler.handleSyncPacket(payload));
                    context.setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(ConfigUpdateRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buffer) -> ConfigUpdateRequestPacket.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> ConfigUpdateRequestPacket.STREAM_CODEC.decode(buffer))
                .consumerNetworkThread((payload, context) -> {
                    ServerPlayer player = context.getSender();
                    context.enqueueWork(() -> ServerPayloadHandler.handleUpdateRequestPacket(payload, player));
                    context.setPacketHandled(true);
                })
                .add();
    }
}