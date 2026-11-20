package dev.henny.vibeyclient.module.inventory.core;

public final class ClickPacer {
    public static final int MIN_RATE = 4, MAX_RATE = 20, DEFAULT_RATE = 12;

    private long gapNanos;
    private long last = Long.MIN_VALUE;

    public ClickPacer(int perSecond) {
        rate(perSecond);
    }

    public void rate(int perSecond) {
        gapNanos = 1_000_000_000L / clamp(perSecond);
    }

    public static int clamp(int perSecond) {
        return Math.max(MIN_RATE, Math.min(MAX_RATE, perSecond));
    }

    public long gapNanos() {
        return gapNanos;
    }

    public boolean ready(long now) {
        return last == Long.MIN_VALUE || now - last >= gapNanos;
    }

    public boolean take(long now) {
        if (!ready(now)) return false;
        last = now;
        return true;
    }
}
