package dev.henny.vibeyclient.module.cinematic.core;

public final class Look {
    private Look() {}

    public static float yaw(V3 from, V3 to) {
        double dx = to.x() - from.x(), dz = to.z() - from.z();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public static float pitch(V3 from, V3 to) {
        V3 d = to.sub(from);
        return (float) -Math.toDegrees(Math.atan2(d.y(), d.horizontal()));
    }

    public static V3 direction(double yawDeg, double pitchDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        double c = Math.cos(p);
        return new V3(-Math.sin(y) * c, -Math.sin(p), Math.cos(y) * c);
    }

    public static double wrap(double deg) {
        double d = deg % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    public static float lerpAngle(float a, float b, double t) {
        return (float) (a + wrap(b - a) * t);
    }
}
