package dev.henny.vibeyclient.module.visuals.items.core;

public final class PoseGestures {
    public static final float DEGREES_PER_PIXEL = 0.6f;
    public static final float SCALE_STEP = 1.08f;
    public static final float DEPTH_STEP = 0.03f;

    private PoseGestures() {}

    public static HandPose move(HandPose pose, int side, float dxPx, float dyPx, float screenHeight) {
        float depth = -HandProjection.anchor(pose, side)[2];
        float unit = 1f / HandProjection.pixelsPerUnit(depth, screenHeight);
        return pose.moved(side * dxPx * unit, -dyPx * unit, 0);
    }

    public static HandPose turn(HandPose pose, int side, float dxPx, float dyPx) {
        return pose.rotated(dyPx * DEGREES_PER_PIXEL, side * dxPx * DEGREES_PER_PIXEL, 0);
    }

    public static HandPose roll(HandPose pose, int side, float dxPx) {
        return pose.rotated(0, 0, -side * dxPx * DEGREES_PER_PIXEL);
    }

    public static HandPose wheelScale(HandPose pose, double steps) {
        return pose.scaled((float) Math.pow(SCALE_STEP, steps));
    }

    public static HandPose wheelDepth(HandPose pose, double steps) {
        return pose.moved(0, 0, (float) (-steps * DEPTH_STEP));
    }
}
