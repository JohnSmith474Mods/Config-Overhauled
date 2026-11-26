package johnsmith.configoverhauled.client.event;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.command.AbstractDataGenCommand;
import johnsmith.configoverhauled.impl.command.ConfigDumpCommand;
import johnsmith.configoverhauled.impl.command.ConfigLangGenCommand;
import johnsmith.configoverhauled.impl.network.client.ClientEventHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.GameShuttingDownEvent;

public class ForgeClientEvents {

    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(ClientEventHandler::onClientDisconnect);
    }

    public static void onClientStopping(GameShuttingDownEvent event) {
        ClientEventHandler.onClientStopping();
    }

    public static void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Constants.COMMAND_ROOT)
                .then(Commands.literal(Constants.COMMAND_LOCALIZATION)
                        .then(Commands.argument(Constants.ARG_MOD_ID, StringArgumentType.word())
                                .suggests(AbstractDataGenCommand::suggestModIds)
                                .executes(context -> {
                                    String modId = StringArgumentType.getString(context, Constants.ARG_MOD_ID);
                                    context.getSource().sendSystemMessage(ConfigLangGenCommand.executeLang(modId, true));
                                    return 1;
                                })
                                .then(Commands.argument(Constants.ARG_ONLY_MISSING_ENTRIES, BoolArgumentType.bool())
                                        .executes(context -> {
                                            String modId = StringArgumentType.getString(context, Constants.ARG_MOD_ID);
                                            boolean onlyMissing = BoolArgumentType.getBool(context, Constants.ARG_ONLY_MISSING_ENTRIES);
                                            context.getSource().sendSystemMessage(ConfigLangGenCommand.executeLang(modId, onlyMissing));
                                            return 1;
                                        }))))
                .then(Commands.literal(Constants.COMMAND_DUMP)
                        .then(Commands.argument(Constants.ARG_MOD_ID, StringArgumentType.word())
                                .suggests(AbstractDataGenCommand::suggestModIds)
                                .then(Commands.argument(Constants.ARG_FORMAT, StringArgumentType.word())
                                        .suggests(ConfigDumpCommand::suggestFormats)
                                        .then(Commands.argument(Constants.ARG_SCOPE, StringArgumentType.word())
                                                .suggests(ConfigDumpCommand::suggestScopes)
                                                .executes(context -> {
                                                    String modId = StringArgumentType.getString(context, Constants.ARG_MOD_ID);
                                                    String format = StringArgumentType.getString(context, Constants.ARG_FORMAT);
                                                    String scope = StringArgumentType.getString(context, Constants.ARG_SCOPE);
                                                    context.getSource().sendSystemMessage(ConfigDumpCommand.executeDump(modId, format, scope));
                                                    return 1;
                                                }))))));
    }
}