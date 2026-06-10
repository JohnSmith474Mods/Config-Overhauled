package johnsmith.configoverhauled.impl.network.common.packet;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConfigUpdateRequestPacket(ConfigDescription metadata, CompoundTag payload) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigUpdateRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("config_overhauled", "update_request"));

    public static final StreamCodec<FriendlyByteBuf, ConfigUpdateRequestPacket> STREAM_CODEC = StreamCodec.ofMember(
            ConfigUpdateRequestPacket::write,
            ConfigUpdateRequestPacket::new
    );

    private ConfigUpdateRequestPacket(FriendlyByteBuf buf) {
        this(
                new ConfigDescription(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf()),
                buf.readNbt()
        );
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.metadata.modId());
        buf.writeUtf(this.metadata.category());
        buf.writeUtf(this.metadata.group());
        buf.writeUtf(this.metadata.property());
        buf.writeNbt(this.payload);
    }

    public static ConfigUpdateRequestPacket create(ConfigDescription metadata, Tag encodedValue) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("value", encodedValue);
        return new ConfigUpdateRequestPacket(metadata, wrapper);
    }

    public Tag getEncodedValue() {
        return this.payload.get("value");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}