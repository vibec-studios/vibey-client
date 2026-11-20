package dev.henny.r3.math;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class Ray {
    private final Vector3d origin = new Vector3d();
    private final Vector3d direction = new Vector3d(0, 0, -1);

    public Ray set(Vector3dc origin, Vector3dc direction) {
        return set(origin.x(), origin.y(), origin.z(), direction.x(), direction.y(), direction.z());
    }

    public Ray set(double ox, double oy, double oz, double dx, double dy, double dz) {
        origin.set(ox, oy, oz);
        direction.set(dx, dy, dz);
        if (direction.lengthSquared() < 1e-18) direction.set(0, 0, -1);
        direction.normalize();
        return this;
    }

    public Ray set(Ray other) {
        origin.set(other.origin);
        direction.set(other.direction);
        return this;
    }

    public Vector3dc origin() {
        return origin;
    }

    public Vector3dc direction() {
        return direction;
    }

    public Vector3d pointAt(double distance, Vector3d out) {
        return out.set(direction).mul(distance).add(origin);
    }
}
