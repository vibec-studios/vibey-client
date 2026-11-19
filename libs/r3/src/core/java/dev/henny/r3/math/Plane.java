package dev.henny.r3.math;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class Plane {
    private final Vector3d point = new Vector3d();
    private final Vector3d normal = new Vector3d(0, 0, 1);

    public Plane set(Vector3dc point, Vector3dc normal) {
        this.point.set(point);
        this.normal.set(normal).normalize();
        return this;
    }

    public Vector3dc point() {
        return point;
    }

    public Vector3dc normal() {
        return normal;
    }

    public double intersect(Ray ray) {
        double denom = ray.direction().dot(normal);
        if (Math.abs(denom) < 1e-9) return Double.NaN;
        double ox = point.x - ray.origin().x();
        double oy = point.y - ray.origin().y();
        double oz = point.z - ray.origin().z();
        double t = (ox * normal.x + oy * normal.y + oz * normal.z) / denom;
        return t < 0 ? Double.NaN : t;
    }

    public double signedDistance(Vector3dc p) {
        return (p.x() - point.x) * normal.x + (p.y() - point.y) * normal.y + (p.z() - point.z) * normal.z;
    }
}
