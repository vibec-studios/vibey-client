package dev.henny.vibeyclient.module.inventory.core;

public final class DurabilityText {
    public static final int GOOD = 0xFF3FE0C5, OK = 0xFFFFE14D, LOW = 0xFFFFB547, CRITICAL = 0xFFFF5470;

    private DurabilityText() {}

    public static int percent(int max, int damage) {
        if (max <= 0) return 100;
        int left = Math.max(0, max - Math.max(0, damage));
        if (left == 0) return 0;
        return Math.max(1, (int) Math.floor(left * 100.0 / max));
    }

    public static int color(int percent) {
        if (percent > 60) return GOOD;
        if (percent > 30) return OK;
        if (percent > 10) return LOW;
        return CRITICAL;
    }
}
