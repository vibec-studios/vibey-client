package dev.henny.vui.mc.fabric;

import dev.henny.vui.mc.dev.DevCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

final class FabricCommands {
    private FabricCommands() {
    }

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
                dispatcher.register(DevCommands.<FabricClientCommandSource>tree((source, message) -> source.sendFeedback(Component.literal(message)))));
    }
}
