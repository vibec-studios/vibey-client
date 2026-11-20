package dev.henny.vibeyclient.module.info.core;

public final class HoldGesture {
    public enum Result { NONE, TAP, HOLD }

    private final int holdTicks;
    private int down;
    private boolean fired;

    public HoldGesture(int holdTicks) {
        this.holdTicks = Math.max(1, holdTicks);
    }

    public Result tick(boolean pressed) {
        if (pressed) {
            down++;
            if (!fired && down >= holdTicks) {
                fired = true;
                return Result.HOLD;
            }
            return Result.NONE;
        }
        Result r = down > 0 && !fired ? Result.TAP : Result.NONE;
        down = 0;
        fired = false;
        return r;
    }

    public float progress() {
        return fired ? 0f : Math.min(1f, down / (float) holdTicks);
    }

    public void reset() {
        down = 0;
        fired = false;
    }
}
