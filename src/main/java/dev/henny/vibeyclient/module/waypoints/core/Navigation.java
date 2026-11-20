package dev.henny.vibeyclient.module.waypoints.core;

public final class Navigation {
    public static final double ARRIVED = 3.0;

    private Navigation() {}

    public static double distance(double px, double py, double pz, Pos target) {
        double dx = target.x() + 0.5 - px, dy = target.y() - py, dz = target.z() + 0.5 - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float relativeYaw(float playerYawDegrees, double dx, double dz) {
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        return wrap((float) (targetYaw - playerYawDegrees));
    }

    public static float wrap(float degrees) {
        float d = degrees % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    public static String distanceText(double blocks) {
        if (blocks < 1000) return Math.round(blocks) + " m";
        long tenths = Math.round(blocks / 100.0);
        return (tenths / 10) + "." + (tenths % 10) + " km";
    }
}
