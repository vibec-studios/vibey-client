package dev.henny.vibeyclient.module.info.core;

public final class Durability {
    public static final int MIN_USES = 3;

    private Durability() {}

    public static int remaining(int max, int damage) {
        return Math.max(0, max - damage);
    }

    public static int threshold(int max, int percent) {
        return Math.max(MIN_USES, (int) Math.ceil(max * Math.max(0, percent) / 100.0));
    }

    public static WarnLevel level(int max, int damage, int percent) {
        if (max <= 0) return WarnLevel.NONE;
        int left = remaining(max, damage);
        int at = threshold(max, percent);
        if (left > at) return WarnLevel.NONE;
        return left <= Math.max(1, at / 3) ? WarnLevel.CRITICAL : WarnLevel.LOW;
    }
}
