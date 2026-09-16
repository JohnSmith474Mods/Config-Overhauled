package johnsmith.configoverhauled.event;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.network.server.ServerEventHandler;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoForgeGameEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerEventHandler.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ServerEventHandler.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ServerEventHandler.onServerStopping(event.getServer());
    }
}