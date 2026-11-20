package dev.henny.vibeyclient.module.info.core;

public final class Flash {
    public static final long SLOW_MS = 1000, FAST_MS = 400;
    public static final float MIN_ALPHA = 0.35f;

    private Flash() {}

    public static long period(WarnLevel level) {
        return level == WarnLevel.CRITICAL ? FAST_MS : SLOW_MS;
    }

    public static float alpha(long nowMs, WarnLevel level) {
        if (level == WarnLevel.NONE) return 0f;
        long period = period(level);
        double phase = (nowMs % period) / (double) period;
        double wave = 0.5 + 0.5 * Math.cos(phase * Math.PI * 2);
        return (float) (MIN_ALPHA + (1 - MIN_ALPHA) * wave);
    }
}
