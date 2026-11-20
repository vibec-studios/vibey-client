package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.CinematicModule;
import net.minecraft.client.Minecraft;

public final class AfkWatch {
    private static volatile long lastInput = System.currentTimeMillis();

    private AfkWatch() {}

    public static void touch() {
        lastInput = System.currentTimeMillis();
    }

    public static void backdate(long ms) {
        lastInput = System.currentTimeMillis() - Math.max(0, ms);
    }

    public static long lastInput() {
        return lastInput;
    }

    public static long idleMillis() {
        return System.currentTimeMillis() - lastInput;
    }

    public static void tick(CinematicModule m) {
        if (Cinematic.active() || !m.auto.on() || !m.allowed()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
        if (mc.player.isDeadOrDying() || mc.player.hurtTime > 0) return;
        if (mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.01) return;
        if (idleMillis() >= m.afkMinutes.get() * 60_000L) Cinematic.start(false);
    }
}
