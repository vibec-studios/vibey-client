package dev.henny.vibeyclient.module.servers.core;

public final class AutoScroll {
    public static final int EDGE = 24;
    public static final float MAX = 9f;

    private AutoScroll() {}

    public static float step(double y, int top, int bottom) {
        if (y < top + EDGE) return -MAX * strength(top + EDGE - y);
        if (y > bottom - EDGE) return MAX * strength(y - (bottom - EDGE));
        return 0f;
    }

    private static float strength(double depth) {
        return (float) Math.min(1.0, Math.max(0.0, depth / EDGE));
    }
}
