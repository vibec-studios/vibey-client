package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.api.GlobalSettings;

/**
 * Time-based eased transition of one float (SPEC §4: 150 ms, cubic-bezier(0.2, 0.8, 0.2, 1)). Create one per animated
 * thing (once, not per frame) and call {@link #to(float)} every frame with the wanted value; it never blocks input
 * and does not allocate. With the global "animations" setting off it jumps straight to the target.
 */
public final class Anim {
    public static final int DURATION_MS = 150;

    private final int durationMs;
    private float from;
    private float to;
    private long start;

    public Anim(float initial) {
        this(initial, DURATION_MS);
    }

    public Anim(float initial, int durationMs) {
        this.from = initial;
        this.to = initial;
        this.durationMs = durationMs;
    }

    /** Sets the target (restarts the transition from the current value if it changed) and returns the value now. */
    public float to(float target) {
        if (target != to) {
            from = value();
            to = target;
            start = System.nanoTime();
        }
        return value();
    }

    /** Jumps to {@code v} without animating. */
    public void snap(float v) {
        from = v;
        to = v;
        start = 0;
    }

    public float target() {
        return to;
    }

    public float value() {
        if (from == to || start == 0 || !GlobalSettings.animate()) return to;
        float t = (System.nanoTime() - start) / (durationMs * 1_000_000f);
        if (t >= 1f) {
            from = to;
            return to;
        }
        return from + (to - from) * ease(t);
    }

    public boolean running() {
        return value() != to;
    }

    /** cubic-bezier(0.2, 0.8, 0.2, 1) at time {@code t} in 0..1. */
    public static float ease(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        // Solve x(s) = t for the curve parameter s (Newton, falls back to bisection), then return y(s).
        float s = t;
        for (int i = 0; i < 6; i++) {
            float x = bezier(s, 0.2f, 0.2f) - t;
            if (Math.abs(x) < 1e-4f) break;
            float d = bezierSlope(s, 0.2f, 0.2f);
            if (Math.abs(d) < 1e-6f) break;
            s -= x / d;
        }
        if (s < 0f || s > 1f || Math.abs(bezier(s, 0.2f, 0.2f) - t) > 1e-3f) {
            float lo = 0f, hi = 1f;
            s = t;
            for (int i = 0; i < 20; i++) {
                float x = bezier(s, 0.2f, 0.2f);
                if (x < t) lo = s;
                else hi = s;
                s = (lo + hi) * 0.5f;
            }
        }
        return bezier(s, 0.8f, 1f);
    }

    private static float bezier(float s, float p1, float p2) {
        float u = 1f - s;
        return 3f * u * u * s * p1 + 3f * u * s * s * p2 + s * s * s;
    }

    private static float bezierSlope(float s, float p1, float p2) {
        float u = 1f - s;
        return 3f * u * u * p1 + 6f * u * s * (p2 - p1) + 3f * s * s * (1f - p2);
    }
}
