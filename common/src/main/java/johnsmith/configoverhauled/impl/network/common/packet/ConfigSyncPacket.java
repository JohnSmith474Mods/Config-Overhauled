package johnsmith.configoverhauled.impl.network.common.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigSyncPacket(String modId, CompoundTag configData) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("config_overhauled", "sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPacket> STREAM_CODEC = StreamCodec.ofMember(
            ConfigSyncPacket::write,
            ConfigSyncPacket::new
    );

    private ConfigSyncPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readNbt());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.modId);
        buf.writeNbt(this.configData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}