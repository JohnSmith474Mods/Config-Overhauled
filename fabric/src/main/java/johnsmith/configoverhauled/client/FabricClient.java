package johnsmith.configoverhauled.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.command.AbstractDataGenCommand;
import johnsmith.configoverhauled.impl.command.ConfigDumpCommand;
import johnsmith.configoverhauled.impl.command.ConfigLangGenCommand;
import johnsmith.configoverhauled.impl.network.client.ClientEventHandler;
import johnsmith.configoverhauled.impl.network.client.ClientPayloadHandler;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientEventHandler.onClientStopping());

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientPayloadHandler.handleSyncPacket(payload));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(ClientEventHandler::onClientDisconnect);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal(Constants.COMMAND_ROOT)
                    .then(ClientCommands.literal(Constants.COMMAND_LOCALIZATION)
                            .then(ClientCommands.argument(Constants.ARG_MOD_ID, StringArgumentType.word())
                                    .suggests(AbstractDataGenCommand::suggestModIds)
                                    .executes(context -> {
                                        String modId = StringArgumentType.getString(context, Constants.ARG_MOD_ID);
                                        context.getSource().sendFeedback(ConfigLangGenCommand.executeLang(modId, true));
                                        return 1;
                                    })
                                    .then(ClientCommands.argument(Constants.ARG_ONLY_MISSING_ENTRIES, BoolArgumentType.bool())
                                            .executes(context -> {
                                                String modId = StringArgumentType.getString(context, Constants.ARG_MOD_ID);
                                                boolean onlyMissing = BoolArgumentType.getBool(context, Constants.ARG_ONLY_MISSING_ENTRIES);
                                                context.getSource().sendFeedback(ConfigLangGenCommand.executeLang(modId, onlyMissing));
                                                return 1;
                                            }))))
                    .then(ClientCommands.literal(Constants.COMMAND_DUMP)
                            .then(ClientCommands.argument(Constants.ARG_MOD_ID, StringArgumentType.word())
                                    .suggests(AbstractDataGenCommand::suggestModIds)
                                    .then(ClientCommands.argument(Constants.ARG_FORMAT, StringArgumentType.word())
                                            .suggests(ConfigDumpCommand::suggestFormats)
                                            .then(ClientCommands.argument(Constants.ARG_SCOPE, StringArgumentType.word())
                                                    .suggests(ConfigDumpCommand::suggestScopes)
                                                    .executes(context -> {
                                                        String modId = StringArgumentType.getString(context, Constants.ARG_MOD_ID);
                                                        String format = StringArgumentType.getString(context, Constants.ARG_FORMAT);
                                                        String scope = StringArgumentType.getString(context, Constants.ARG_SCOPE);
                                                        context.getSource().sendFeedback(ConfigDumpCommand.executeDump(modId, format, scope));
                                                        return 1;
                                                    }))))));
        });
    }
}