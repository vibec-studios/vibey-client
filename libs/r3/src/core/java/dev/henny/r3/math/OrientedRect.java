package dev.henny.r3.math;

import dev.henny.r3.object.Transform;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class OrientedRect {
    private final Vector3d center = new Vector3d();
    private final Vector3d right = new Vector3d(1, 0, 0);
    private final Vector3d up = new Vector3d(0, 1, 0);
    private final Vector3d normal = new Vector3d(0, 0, 1);
    private final Vector3d scratch = new Vector3d();
    private double halfWidth = 0.5;
    private double halfHeight = 0.5;

    public OrientedRect set(Transform t) {
        return set(t.position(), t.orientation(), t.worldWidth() / 2.0, t.worldHeight() / 2.0);
    }

    public OrientedRect set(Vector3dc center, Quaternionfc orientation, double halfWidth, double halfHeight) {
        this.center.set(center);
        Axes.right(orientation, right);
        Axes.up(orientation, up);
        Axes.normal(orientation, normal);
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        return this;
    }

    public OrientedRect set(OrientedRect other) {
        center.set(other.center);
        right.set(other.right);
        up.set(other.up);
        normal.set(other.normal);
        halfWidth = other.halfWidth;
        halfHeight = other.halfHeight;
        return this;
    }

    public Vector3dc center() {
        return center;
    }

    public Vector3dc right() {
        return right;
    }

    public Vector3dc up() {
        return up;
    }

    public Vector3dc normal() {
        return normal;
    }

    public double halfWidth() {
        return halfWidth;
    }

    public double halfHeight() {
        return halfHeight;
    }

    public boolean hit(Ray ray, double margin, boolean doubleSided, double maxDistance, RectHit out) {
        double denom = ray.direction().dot(normal);
        if (Math.abs(denom) < 1e-9) return false;
        if (denom > 0 && !doubleSided) return false;
        double t = scratch.set(center).sub(ray.origin()).dot(normal) / denom;
        if (t < 0 || t > maxDistance) return false;
        if (!local(ray, t, denom < 0, out)) return false;
        return Math.abs(out.localX()) <= halfWidth + margin && Math.abs(out.localY()) <= halfHeight + margin;
    }

    public boolean planeHit(Ray ray, RectHit out) {
        double denom = ray.direction().dot(normal);
        if (Math.abs(denom) < 1e-9) return false;
        double t = scratch.set(center).sub(ray.origin()).dot(normal) / denom;
        return t >= 0 && local(ray, t, denom < 0, out);
    }

    private boolean local(Ray ray, double t, boolean front, RectHit out) {
        Vector3d p = ray.pointAt(t, scratch);
        double dx = p.x - center.x, dy = p.y - center.y, dz = p.z - center.z;
        double lx = dx * right.x + dy * right.y + dz * right.z;
        double ly = dx * up.x + dy * up.y + dz * up.z;
        out.set(p, t, lx, ly, lx / (2 * halfWidth) + 0.5, 0.5 - ly / (2 * halfHeight), front);
        return true;
    }

    public Vector3d corner(int sx, int sy, Vector3d out) {
        return out.set(center).fma(sx * halfWidth, right).fma(sy * halfHeight, up);
    }
}
