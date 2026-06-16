package johnsmith.configoverhauled.platform;


import johnsmith.configoverhauled.impl.platform.services.IPlatformHelper;
import johnsmith.configoverhauled.network.ForgeNetworkRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() { return "Forge";}

    @Override
    public boolean isModLoaded(String modId) { return ModList.getMods().stream().anyMatch(
            modInfo -> modInfo.getModId().equals(modId)); }

    @Override
    public boolean isDevelopmentEnvironment() { return !FMLLoader.isProduction(); }

    @Override
    public void sendToClient(CustomPacketPayload payload, ServerPlayer player) {
        ForgeNetworkRegistration.CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ForgeNetworkRegistration.CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendToAllClients(CustomPacketPayload payload, MinecraftServer server) {
        ForgeNetworkRegistration.CHANNEL.send(payload, PacketDistributor.ALL.noArg());
    }
}