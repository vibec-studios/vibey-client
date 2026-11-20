package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.CinematicModule;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.plan.Director;
import dev.henny.vibeyclient.module.cinematic.core.plan.Mode;
import dev.henny.vibeyclient.module.cinematic.core.plan.Scene;
import dev.henny.vibeyclient.module.cinematic.core.plan.Session;
import net.minecraft.client.Minecraft;

public final class Cinematic {
    private static final SceneSource SCENES = new SceneSource();
    private static Session session;
    private static Session.Frame frame;
    private static boolean preview;
    private static long startedAt;
    private static long lastNanos;

    private Cinematic() {}

    public static boolean active() {
        return session != null;
    }

    public static boolean preview() {
        return session != null && preview;
    }

    public static boolean running() {
        return session != null && session.running();
    }

    public static Session.Frame frame() {
        return frame;
    }

    public static long startedAt() {
        return startedAt;
    }

    public static Director director() {
        return session == null ? null : session.director();
    }

    public static Mode playing() {
        Director d = director();
        CinematicModule m = CinematicModule.get();
        return d != null ? d.playing() : m != null ? m.mode.get() : Mode.MIX;
    }

    public static boolean start(boolean asPreview) {
        Minecraft mc = Minecraft.getInstance();
        CinematicModule m = CinematicModule.get();
        if (session != null || m == null || !m.allowed() || mc.player == null || mc.level == null) return false;
        Director d = new Director(System.nanoTime());
        d.mode(m.mode.get());
        d.mixSeconds(m.mixSeconds.get());
        session = new Session(d);
        frame = null;
        preview = asPreview;
        startedAt = AfkWatch.lastInput();
        lastNanos = 0;
        InputGate.armed(mc);
        if (!preview) HudSwap.enter();
        return true;
    }

    public static void startFromMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.setScreen(null);
        start(false);
    }

    public static void toggleFromKey() {
        if (session != null) stop();
        else start(false);
    }

    public static void stop() {
        if (session == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            abort();
            return;
        }
        session.stop(scene(mc));
    }

    public static void abort() {
        if (session == null) return;
        session = null;
        frame = null;
        SCENES.clear();
        if (!preview) HudSwap.leave();
        preview = false;
    }

    public static void modeChanged(Mode m) {
        if (session != null) session.director().mode(m);
    }

    public static void rulesChanged() {
        CinematicModule m = CinematicModule.get();
        if (session != null && (m == null || !m.allowed())) abort();
    }

    public static void tick() {
        if (session == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            abort();
            return;
        }
        if (preview) return;
        if (mc.screen != null || mc.player.hurtTime > 0 || mc.player.isDeadOrDying()) stop();
    }

    public static Session.Frame update(Minecraft mc, Pose player, float partial) {
        if (session == null) return null;
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        SceneSource.partial = partial;
        CinematicModule m = CinematicModule.get();
        if (m != null) session.director().mixSeconds(m.mixSeconds.get());
        frame = session.update(scene(mc), player, dt);
        if (frame.done()) {
            Session.Frame last = frame;
            abort();
            return last;
        }
        return frame;
    }

    private static Scene scene(Minecraft mc) {
        CinematicModule m = CinematicModule.get();
        int radius = m == null ? 32 : Math.min(m.radius.get(), Reach.limit(mc));
        double speed = m == null ? 1 : m.speed.get();
        return SCENES.get(mc, radius, speed);
    }
}
