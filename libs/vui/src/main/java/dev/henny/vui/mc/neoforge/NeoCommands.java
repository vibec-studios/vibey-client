package dev.henny.vui.mc.neoforge;

import dev.henny.vui.mc.dev.DevCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

final class NeoCommands {
    private NeoCommands() {
    }

    static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(DevCommands.<CommandSourceStack>tree((source, message) -> source.sendSystemMessage(Component.literal(message))));
    }
}
