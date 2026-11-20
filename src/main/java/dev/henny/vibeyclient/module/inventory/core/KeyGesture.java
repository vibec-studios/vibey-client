package dev.henny.vibeyclient.module.inventory.core;

public final class KeyGesture {
    public enum Result { NONE, TAP, HOLD_END }

    private final long holdNanos;
    private boolean down;
    private long since;
    private boolean used;

    public KeyGesture(long holdMillis) {
        this.holdNanos = holdMillis * 1_000_000L;
    }

    public Result update(boolean pressed, long now) {
        if (pressed && !down) {
            down = true;
            since = now;
            used = false;
            return Result.NONE;
        }
        if (!pressed && down) {
            boolean held = holding(now);
            down = false;
            return held ? Result.HOLD_END : Result.TAP;
        }
        return Result.NONE;
    }

    public boolean holding(long now) {
        return down && (used || now - since >= holdNanos);
    }

    public boolean down() {
        return down;
    }

    public void markUsed() {
        if (down) used = true;
    }

    public void reset() {
        down = false;
        used = false;
    }
}
