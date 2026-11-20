package dev.henny.companio.mc.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.mc.client.CompanioClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class CompanioCommand {
    private CompanioCommand() {}

    public static LiteralArgumentBuilder<FabricClientCommandSource> tree() {
        return ClientCommandManager.literal("companio")
                .then(ClientCommandManager.literal("packs").executes(c -> run(CompanioMc::openPacks)))
                .then(ClientCommandManager.literal("reload").executes(c -> run(CompanioMc::reload)))
                .then(ClientCommandManager.literal("debug").executes(c -> run(CompanioMc::toggleDebug)))
                .then(ClientCommandManager.literal("menu").executes(c -> run(CompanioMc::openMenu)))
                .then(ClientCommandManager.literal("recall").executes(c -> run(CompanioMc::recall)))
                .then(ClientCommandManager.literal("hide").executes(c -> run(() -> CompanioMc.select(""))))
                .then(ClientCommandManager.literal("summon").then(ClientCommandManager.argument("companion", StringArgumentType.greedyString())
                        .executes(c -> {
                            String key = StringArgumentType.getString(c, "companion").trim();
                            boolean ok = CompanioMc.select(key);
                            c.getSource().sendFeedback(Component.translatable(ok ? "companio.command.summoned" : "companio.command.unknown", key));
                            return ok ? 1 : 0;
                        })))
                .then(ClientCommandManager.literal("trick").then(ClientCommandManager.argument("trick", StringArgumentType.word())
                        .executes(c -> CompanioMc.trick(StringArgumentType.getString(c, "trick")) ? 1 : 0)));
    }

    private static int run(Runnable r) {
        if (CompanioClient.get() == null) return 0;
        r.run();
        return 1;
    }
}
