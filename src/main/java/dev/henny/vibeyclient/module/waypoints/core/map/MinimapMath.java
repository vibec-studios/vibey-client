package dev.henny.vibeyclient.module.waypoints.core.map;

public final class MinimapMath {
    private MinimapMath() {}

    public static float rotation(float yawDegrees, boolean rotate) {
        return rotate ? (float) (Math.PI - Math.toRadians(yawDegrees)) : 0f;
    }

    public static boolean project(double dx, double dz, float rotation, double zoom, double half, float[] out) {
        double c = Math.cos(rotation), s = Math.sin(rotation);
        double sx = (c * dx - s * dz) * zoom;
        double sy = (s * dx + c * dz) * zoom;
        double m = Math.max(Math.abs(sx), Math.abs(sy));
        boolean inside = m <= half;
        if (!inside) {
            sx *= half / m;
            sy *= half / m;
        }
        out[0] = (float) sx;
        out[1] = (float) sy;
        return inside;
    }
}
