package dev.henny.vibeyclient.module.visuals.items.core;

public final class HandProjection {
    public static final float FOV_DEGREES = 70f;
    public static final float BASE_X = 0.56f, BASE_Y = -0.52f, BASE_Z = -0.72f;
    public static final float PIVOT_X = 0.05f, PIVOT_Y = 0.16f, PIVOT_Z = 0.05f;
    public static final float ITEM_SIZE = 0.5f;
    private static final float TAN = (float) Math.tan(Math.toRadians(FOV_DEGREES / 2));

    private HandProjection() {}

    public static float[] anchor(HandPose pose, int side) {
        float x = side * (BASE_X + pose.x() + PIVOT_X);
        float y = BASE_Y + pose.y() + PIVOT_Y;
        float z = BASE_Z + pose.z() + PIVOT_Z;
        return new float[] {x, y, Math.min(-0.05f, z)};
    }

    public static float[] toScreen(float[] p, float width, float height) {
        float depth = -p[2];
        float aspect = width / height;
        float nx = p[0] / depth / (TAN * aspect);
        float ny = p[1] / depth / TAN;
        return new float[] {(nx + 1) / 2 * width, (1 - ny) / 2 * height};
    }

    public static float pixelsPerUnit(float depth, float height) {
        return height / (2 * TAN * Math.max(0.05f, depth));
    }

    public static float radius(HandPose pose, int side, float height) {
        float[] p = anchor(pose, side);
        return ITEM_SIZE * pose.scale() * pixelsPerUnit(-p[2], height) / 2;
    }
}
