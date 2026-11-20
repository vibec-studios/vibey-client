package dev.henny.vibeyclient.module.visuals.effects.core;

public final class PreviewCurve {
    public static final long FADE_MS = 400;

    private PreviewCurve() {}

    public static float level(long startMs, long durationMs, long nowMs) {
        long t = nowMs - startMs;
        if (startMs <= 0 || t < 0 || t >= durationMs) return 0;
        float in = Math.min(1f, t / (float) FADE_MS);
        float out = Math.min(1f, (durationMs - t) / (float) FADE_MS);
        return Math.min(in, out);
    }

    public static boolean active(long startMs, long durationMs, long nowMs) {
        return startMs > 0 && nowMs >= startMs && nowMs - startMs < durationMs;
    }
}
