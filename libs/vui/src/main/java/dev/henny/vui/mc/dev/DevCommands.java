package dev.henny.vui.mc.dev;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.henny.vui.dom.Document;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.compat.Mc;

import java.util.function.BiConsumer;

public final class DevCommands {
    private DevCommands() {
    }

    public static <S> LiteralArgumentBuilder<S> tree(BiConsumer<S, String> feedback) {
        VuiRuntime rt = VuiRuntime.get();
        return LiteralArgumentBuilder.<S>literal("vui")
                .then(LiteralArgumentBuilder.<S>literal("open")
                        .then(RequiredArgumentBuilder.<S, String>argument("id", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id").trim();
                                    return run(ctx.getSource(), feedback, "vui: opening " + id, () -> rt.dev().open(id));
                                })))
                .then(LiteralArgumentBuilder.<S>literal("demo")
                        .executes(ctx -> run(ctx.getSource(), feedback, "vui: opening " + DevTools.DEMO, () -> rt.dev().open(DevTools.DEMO))))
                .then(LiteralArgumentBuilder.<S>literal("reload")
                        .executes(ctx -> run(ctx.getSource(), feedback, "vui: reloading " + rt.vui().reloader().documents().size() + " documents",
                                () -> rt.vui().reloader().reloadAll())))
                .then(LiteralArgumentBuilder.<S>literal("hud")
                        .executes(ctx -> run(ctx.getSource(), feedback, "vui: HUD editor", () -> {
                            if (rt.hud().layer().widgets().isEmpty()) rt.dev().demoHud();
                            rt.hud().openEditor();
                        })))
                .then(LiteralArgumentBuilder.<S>literal("inspect")
                        .executes(ctx -> run(ctx.getSource(), feedback, "vui: inspector toggled (Ctrl+Shift+I)", () -> rt.inspector().toggle())))
                .then(LiteralArgumentBuilder.<S>literal("errors")
                        .executes(ctx -> {
                            int errors = 0;
                            for (Document d : rt.vui().reloader().documents()) errors += d.diagnostics().errors().size();
                            feedback.accept(ctx.getSource(), "vui: " + errors + " errors, bridge port " + rt.dev().bridgePort());
                            return 1;
                        }));
    }

    private static <S> int run(S source, BiConsumer<S, String> feedback, String message, Runnable action) {
        feedback.accept(source, message);
        Mc.later(action);
        return 1;
    }
}
