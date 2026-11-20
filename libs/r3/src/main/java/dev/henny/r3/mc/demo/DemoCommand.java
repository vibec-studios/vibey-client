package dev.henny.r3.mc.demo;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.henny.r3.mc.R3Mod;

public final class DemoCommand {
    public static final String ROOT = "r3";

    private DemoCommand() {}

    public static <S> LiteralArgumentBuilder<S> tree() {
        return LiteralArgumentBuilder.<S>literal(ROOT)
                .then(LiteralArgumentBuilder.<S>literal("demo").executes(c -> demo()))
                .then(LiteralArgumentBuilder.<S>literal("clear").executes(c -> clear()));
    }

    static int demo() {
        boolean placed = R3Demo.start();
        if (!placed) R3Mod.LOG.info("r3 demo: look at a block face within {} blocks", (int) R3Demo.REACH);
        return placed ? 1 : 0;
    }

    static int clear() {
        R3Demo.stop();
        return 1;
    }
}
