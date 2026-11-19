package dev.henny.vibeyclient.module.clips;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** What the mixins call; cheap no-ops while the module is off. */
public final class ClipsHooks {
    private ClipsHooks() {}

    /** Render thread, right after {@code blitToScreen} in {@code Minecraft.runTick}. */
    public static void afterBlit() {
        if (frameTimes != null) sample();
        ClipsModule m = ClipsModule.get();
        if (m != null && m.isEnabled()) m.recorder.onFrame();
    }

    // ---------------------------------------------------------------------------------------- frame times (tests)
    private static volatile long[] frameTimes;
    private static int frameCount;
    private static long lastFrame;

    /** Starts recording frame intervals (game test FPS measurement). */
    public static void measureFrames(int max) {
        frameCount = 0;
        lastFrame = 0;
        frameTimes = new long[max];
    }

    /** Stops and returns the recorded frame intervals in ns. */
    public static long[] stopMeasuring() {
        long[] t = frameTimes;
        frameTimes = null;
        return t == null ? new long[0] : java.util.Arrays.copyOf(t, Math.min(frameCount, t.length));
    }

    private static void sample() {
        long now = System.nanoTime();
        long[] t = frameTimes;
        if (lastFrame != 0 && t != null && frameCount < t.length) t[frameCount++] = now - lastFrame;
        lastFrame = now;
    }

    public static void damage(Entity victim, int causeId, int directId) {
        ClipsModule m = ClipsModule.get();
        if (m != null && m.isEnabled()) m.auto.onDamage(victim, causeId, directId);
    }

    public static void entityEvent(Entity e, byte event) {
        if (event != 3) return;
        ClipsModule m = ClipsModule.get();
        if (m != null && m.isEnabled()) m.auto.onDeathEvent(e);
    }

    public static void attack(Entity target) {
        ClipsModule m = ClipsModule.get();
        if (m != null && m.isEnabled()) m.auto.onAttack(target);
    }

    public static void gameMessage(Component message, boolean overlay) {
        if (overlay) return;
        ClipsModule m = ClipsModule.get();
        if (m != null && m.isEnabled()) m.auto.onGameMessage(message);
    }
}
