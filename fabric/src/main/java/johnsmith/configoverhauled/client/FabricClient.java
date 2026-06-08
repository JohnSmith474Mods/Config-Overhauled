package johnsmith.configoverhauled.client;

import com.mojang.brigadier.arguments.StringArgumentType;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.impl.client.gui.registry.ConfigWidgetRegistry;
import johnsmith.configoverhauled.impl.command.AbstractDataGenCommand;
import johnsmith.configoverhauled.impl.command.ConfigDumpCommand;
import johnsmith.configoverhauled.impl.command.ConfigLangGenCommand;
import johnsmith.configoverhauled.impl.network.client.ClientEventHandler;
import johnsmith.configoverhauled.impl.network.client.ClientPayloadHandler;
import johnsmith.configoverhauled.impl.network.common.packet.ConfigSyncPacket;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ConfigWidgetRegistry.registerWidgets();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientEventHandler.onClientStopping());

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientPayloadHandler.handleSyncPacket(payload));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(ClientEventHandler::onClientDisconnect);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal(Constants.CONFIG_DUMP)
                    .then(ClientCommandManager.argument(Constants.NAMESPACE, StringArgumentType.word())
                            .suggests(AbstractDataGenCommand::suggestModIds)
                            .executes(context -> {
                                String modId = StringArgumentType.getString(context, Constants.NAMESPACE);
                                context.getSource().sendFeedback(ConfigDumpCommand.executeDump(modId));
                                return 1;
                            })));
            dispatcher.register(ClientCommandManager.literal(Constants.CONFIG_LANG_GEN)
                    .then(ClientCommandManager.argument(Constants.NAMESPACE, StringArgumentType.word())
                            .suggests(AbstractDataGenCommand::suggestModIds)
                            .executes(context -> {
                                String modId = StringArgumentType.getString(context, Constants.NAMESPACE);
                                context.getSource().sendFeedback(ConfigLangGenCommand.executeLang(modId));
                                return 1;
                            })));
        });
    }
}