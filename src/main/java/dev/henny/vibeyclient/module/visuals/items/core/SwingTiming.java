package dev.henny.vibeyclient.module.visuals.items.core;

public final class SwingTiming {
    public static final int MIN_PERCENT = 25, MAX_PERCENT = 300;

    private SwingTiming() {}

    public static int duration(int vanillaTicks, int speedPercent) {
        if (speedPercent == 100 || vanillaTicks <= 0) return vanillaTicks;
        int pct = Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, speedPercent));
        return Math.max(1, Math.round(vanillaTicks * 100f / pct));
    }
}
