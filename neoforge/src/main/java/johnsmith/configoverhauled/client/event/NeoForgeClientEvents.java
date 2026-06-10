package johnsmith.configoverhauled.client.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.command.AbstractDataGenCommand;
import johnsmith.configoverhauled.impl.command.ConfigDumpCommand;
import johnsmith.configoverhauled.impl.command.ConfigLangGenCommand;
import johnsmith.configoverhauled.impl.network.client.ClientEventHandler;

import net.minecraft.client.Minecraft;

import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class NeoForgeClientEvents {

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(ClientEventHandler::onClientDisconnect);
    }

    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Constants.CONFIG_DUMP)
                .then(Commands.argument(Constants.NAMESPACE, StringArgumentType.word())
                        .suggests(AbstractDataGenCommand::suggestModIds)
                        .executes(context -> {
                            String modId = StringArgumentType.getString(context, Constants.NAMESPACE);
                            context.getSource().sendSystemMessage(ConfigDumpCommand.executeDump(modId));
                            return 1;
                        })));
        event.getDispatcher().register(Commands.literal(Constants.CONFIG_LANG_GEN)
                .then(Commands.argument(Constants.NAMESPACE, StringArgumentType.word())
                        .suggests(AbstractDataGenCommand::suggestModIds)
                        .executes(context -> {
                            String modId = StringArgumentType.getString(context, Constants.NAMESPACE);
                            context.getSource().sendSystemMessage(ConfigLangGenCommand.executeLang(modId));
                            return 1;
                        })));
    }
}