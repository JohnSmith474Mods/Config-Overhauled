package johnsmith.configoverhauled.event;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.network.server.ServerEventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class ForgeGameEvents {

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
