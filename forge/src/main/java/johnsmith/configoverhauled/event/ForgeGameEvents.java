package johnsmith.configoverhauled.event;

import johnsmith.configoverhauled.impl.network.server.ServerEventHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

public class ForgeGameEvents {

    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerEventHandler.onPlayerJoin(player);
        }
    }

    public static void onServerStarting(ServerStartingEvent event) {
        ServerEventHandler.onServerStarting(event.getServer());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        ServerEventHandler.onServerStopping(event.getServer());
    }
}