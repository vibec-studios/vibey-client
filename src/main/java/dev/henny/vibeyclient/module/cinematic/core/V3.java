package dev.henny.vibeyclient.module.cinematic.core;

public record V3(double x, double y, double z) {
    public static final V3 ZERO = new V3(0, 0, 0);
    public static final V3 UP = new V3(0, 1, 0);

    public V3 add(V3 o) {
        return new V3(x + o.x, y + o.y, z + o.z);
    }

    public V3 add(double dx, double dy, double dz) {
        return new V3(x + dx, y + dy, z + dz);
    }

    public V3 sub(V3 o) {
        return new V3(x - o.x, y - o.y, z - o.z);
    }

    public V3 mul(double s) {
        return new V3(x * s, y * s, z * s);
    }

    public double dot(V3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double horizontal() {
        return Math.sqrt(x * x + z * z);
    }

    public double dist(V3 o) {
        return sub(o).length();
    }

    public V3 norm() {
        double l = length();
        return l < 1e-9 ? ZERO : mul(1 / l);
    }

    public V3 lerp(V3 o, double t) {
        return new V3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    public static V3 heading(double yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new V3(-Math.sin(r), 0, Math.cos(r));
    }

    public V3 side() {
        return new V3(-z, 0, x).norm();
    }

    public boolean finite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}
