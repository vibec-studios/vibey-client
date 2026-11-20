package dev.henny.vibeyclient.module.camera.core;

public final class OrbitOffset {
    public static final float MIN_ZOOM = 0.5f;
    public static final float MAX_ZOOM = 3f;
    public static final float ZOOM_STEP = 1.15f;

    private float yaw;
    private float pitch;
    private float zoom = 1f;

    public void drag(double dx, double dy, float basePitch) {
        yaw += (float) (dx * LookAngles.DEGREES_PER_UNIT);
        float total = LookAngles.clampPitch(basePitch + pitch + (float) (dy * LookAngles.DEGREES_PER_UNIT));
        pitch = total - basePitch;
    }

    public void wheel(double notches) {
        zoom = clampZoom((float) (zoom * Math.pow(ZOOM_STEP, -notches)));
    }

    public float yaw(float baseYaw) {
        return baseYaw + yaw;
    }

    public float pitch(float basePitch) {
        return LookAngles.clampPitch(basePitch + pitch);
    }

    public float zoom() {
        return zoom;
    }

    public float yawOffset() {
        return yaw;
    }

    public float pitchOffset() {
        return pitch;
    }

    public boolean idle() {
        return yaw == 0f && pitch == 0f && zoom == 1f;
    }

    public void reset() {
        yaw = 0f;
        pitch = 0f;
        zoom = 1f;
    }

    public static float clampZoom(float z) {
        if (Float.isNaN(z)) return 1f;
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
    }
}
