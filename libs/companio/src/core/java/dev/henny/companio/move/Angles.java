package dev.henny.companio.move;

public final class Angles {
    private Angles() {}

    public static float wrap(float deg) {
        float d = deg % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    public static float approach(float from, float to, float maxStep) {
        float d = wrap(to - from);
        if (Math.abs(d) <= maxStep) return to;
        return from + Math.signum(d) * maxStep;
    }

    public static float yawTo(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public static float lerp(float t, float a, float b) {
        return a + wrap(b - a) * t;
    }
}
