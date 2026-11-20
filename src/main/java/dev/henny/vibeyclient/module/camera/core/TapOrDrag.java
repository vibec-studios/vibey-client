package dev.henny.vibeyclient.module.camera.core;

public final class TapOrDrag {
    public static final double MAX_TAP_TRAVEL = 15;
    public static final long MAX_TAP_MS = 300;

    private boolean down;
    private double travel;
    private long downAt;

    public void press(long nowMs) {
        down = true;
        travel = 0;
        downAt = nowMs;
    }

    public void move(double dx, double dy) {
        if (down) travel += Math.abs(dx) + Math.abs(dy);
    }

    public boolean release(long nowMs) {
        if (!down) return false;
        down = false;
        return travel <= MAX_TAP_TRAVEL && nowMs - downAt <= MAX_TAP_MS;
    }

    public void cancel() {
        down = false;
    }

    public boolean down() {
        return down;
    }

    public boolean dragging() {
        return down && travel > MAX_TAP_TRAVEL;
    }
}
