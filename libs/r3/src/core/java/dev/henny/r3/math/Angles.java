package dev.henny.r3.math;

import org.joml.Vector3d;

public final class Angles {
    private Angles() {}

    public static float wrapDegrees(float degrees) {
        float d = degrees % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    public static Vector3d direction(float yawDegrees, float pitchDegrees, Vector3d out) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double c = Math.cos(pitch);
        return out.set(-Math.sin(yaw) * c, -Math.sin(pitch), Math.cos(yaw) * c);
    }

    public static float yawOf(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public static float pitchOf(double dx, double dy, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
    }
}
