package dev.henny.vibeyclient.module.camera.core;

public final class LookAngles {
    public static final float MAX_PITCH = 90f;
    public static final float DEGREES_PER_UNIT = 0.15f;

    private float yaw;
    private float pitch;

    public void reset(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = clampPitch(pitch);
    }

    public void turn(double dx, double dy) {
        yaw += (float) (dx * DEGREES_PER_UNIT);
        pitch = clampPitch(pitch + (float) (dy * DEGREES_PER_UNIT));
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public static float clampPitch(float pitch) {
        if (Float.isNaN(pitch)) return 0f;
        return Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
    }
}
