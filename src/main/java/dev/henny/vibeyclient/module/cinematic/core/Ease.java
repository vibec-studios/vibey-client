package dev.henny.vibeyclient.module.cinematic.core;

public final class Ease {
    private Ease() {}

    public static double clamp01(double t) {
        return t < 0 ? 0 : t > 1 ? 1 : t;
    }

    public static double smooth(double t) {
        t = clamp01(t);
        return t * t * (3 - 2 * t);
    }

    public static double smoother(double t) {
        t = clamp01(t);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public static double inOutSine(double t) {
        return -(Math.cos(Math.PI * clamp01(t)) - 1) / 2;
    }

    public static double outCubic(double t) {
        double u = 1 - clamp01(t);
        return 1 - u * u * u;
    }

    public static double gentle(double t) {
        return 0.25 * clamp01(t) + 0.75 * inOutSine(t);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double approach(double current, double target, double rate, double dt) {
        return target + (current - target) * Math.exp(-rate * dt);
    }
}
