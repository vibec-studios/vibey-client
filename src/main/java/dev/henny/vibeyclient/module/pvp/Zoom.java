package dev.henny.vibeyclient.module.pvp;

/**
 * Hold-to-zoom. The zoom never touches the player's options: {@code GameRendererMixin} divides the world FOV by
 * {@link #factor()} and {@code MouseHandlerMixin} scales the mouse turn by {@link #mouseScale()}, so letting go of the
 * key gives back exactly the FOV and mouse speed from before. The factor eases in log space (zooming in and out feel
 * the same) with a time constant from {@code zoomTime}; the mouse wheel steps the level while the key is held.
 */
public final class Zoom {
    private Zoom() {}

    private static final double MIN = 1.25, MAX = 50;

    private static boolean held;
    private static double scrollMul = 1;
    private static double current = 1;
    private static long lastNanos;

    /** The zoom factor right now (1 = no zoom). Called from GameRenderer.getFov, possibly several times a frame. */
    public static double factor() {
        PvpModule m = PvpModule.get();
        boolean on = m != null && m.zoomActive() && m.zoomKey.isDown();
        if (on) {
            held = true;
        } else if (held) {
            held = false;
            scrollMul = 1; // a scrolled level is only for this hold
        }
        double target = on ? clamp(m.zoomLevel.get() * scrollMul) : 1;
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.25, (now - lastNanos) / 1e9);
        lastNanos = now;
        if (m == null || !m.zoomSmooth.on()) {
            current = target;
        } else if (current != target) {
            double tau = m.zoomTime.get() / 1000.0 / 3.0; // ~95 % of the way after zoomTime
            double k = 1 - Math.exp(-dt / Math.max(1e-3, tau));
            double lc = Math.log(current), lt = Math.log(target);
            lc += (lt - lc) * k;
            current = Math.abs(lt - lc) < 0.002 ? target : Math.exp(lc);
        }
        return current;
    }

    /** Multiplier for the mouse turn: slower in proportion to the zoom, so aiming feels the same. */
    public static double mouseScale() {
        PvpModule m = PvpModule.get();
        if (m == null || !m.zoomSensitivity.on() || current <= 1.0001) return 1;
        return 1 / current;
    }

    /** Cinematic (smoothed) camera while zoomed, if the player wants it. */
    public static boolean cinematic() {
        PvpModule m = PvpModule.get();
        return m != null && m.zoomCinematic.on() && current > 1.05 && m.zoomActive();
    }

    /** Mouse wheel while zooming: steps the level and returns true (the hotbar does not move). */
    public static boolean scroll(double amount) {
        PvpModule m = PvpModule.get();
        if (!held || m == null || !m.zoomScroll.on() || !m.zoomActive() || amount == 0) return false;
        double step = m.zoomScrollStep.get();
        double base = m.zoomLevel.get();
        double next = scrollMul * (amount > 0 ? step : 1 / step);
        scrollMul = clamp(base * next) / base;
        return true;
    }

    /** Is the key held right now (the zoom may still be easing)? */
    public static boolean held() {
        return held;
    }

    /** Back to no zoom at once (feature or module switched off, server rule, world left). */
    public static void reset() {
        held = false;
        scrollMul = 1;
        current = 1;
    }

    private static double clamp(double v) {
        return Math.max(MIN, Math.min(MAX, v));
    }
}
