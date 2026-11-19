package dev.henny.vibeyclient.net;

import com.mojang.brigadier.CommandDispatcher;
import dev.henny.vibeyclient.Vibey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /vibeyrules reload|status} on dedicated servers (operators, permission level 2). */
public final class RulesCommand {
    private RulesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (Vibey.platform().isClient()) return; // integrated servers do not send rules
        dispatcher.register(Commands.literal("vibeyrules")
                //? if >=1.21.11 {
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                //?} else
                /*.requires(src -> src.hasPermission(2))*/
                .then(Commands.literal("reload").executes(ctx -> {
                    String line = ServerRulesHost.reload();
                    ServerRulesHost.resendAll(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal(line), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    for (String l : ServerRulesHost.status(ctx.getSource().getServer())) {
                        ctx.getSource().sendSuccess(() -> Component.literal(l), false);
                    }
                    return 1;
                })));
    }
}
