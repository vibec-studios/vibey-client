package dev.henny.r3.mc.input;

import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.scene.R3Scene;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;

public final class VanillaGuard {
    private VanillaGuard() {}

    public static void startTick(R3Scene scene) {
        if (Game.mc().player == null || !scene.blocksVanillaMouse()) return;
        Options o = Game.mc().options;
        drop(o.keyAttack);
        drop(o.keyUse);
        drop(o.keyPickItem);
    }

    private static void drop(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            continue;
        }
        mapping.setDown(false);
    }
}
