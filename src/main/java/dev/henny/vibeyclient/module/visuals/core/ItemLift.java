package dev.henny.vibeyclient.module.visuals.core;

public final class ItemLift {
    public static final float GAP = 0.01f;
    public static final float FLAT_DEPTH = 0.0625f;
    public static final float STACK_SPACING = 1.5f;

    private ItemLift() {}

    public static boolean flat(float depth) {
        return depth <= FLAT_DEPTH;
    }

    public static float stackDepth(float depth, int rendered) {
        if (!flat(depth)) return depth;
        return depth + depth * STACK_SPACING * Math.max(0, rendered - 1);
    }

    public static float lift(float rotationX, float height, float depth) {
        double c = Math.abs(Math.cos(rotationX)), s = Math.abs(Math.sin(rotationX));
        return (float) (0.5 * (c * height + s * depth)) + GAP;
    }
}
