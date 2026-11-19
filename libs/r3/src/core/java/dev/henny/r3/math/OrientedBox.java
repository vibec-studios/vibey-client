package dev.henny.r3.math;

import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class OrientedBox {
    private final Vector3d center = new Vector3d();
    private final Vector3d[] axes = {new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 1)};
    private final double[] half = {0.5, 0.5, 0.5};
    private final Vector3d scratch = new Vector3d();

    public OrientedBox set(Vector3dc center, Quaternionfc orientation, double halfX, double halfY, double halfZ) {
        this.center.set(center);
        Axes.right(orientation, axes[0]);
        Axes.up(orientation, axes[1]);
        Axes.normal(orientation, axes[2]);
        half[0] = halfX;
        half[1] = halfY;
        half[2] = halfZ;
        return this;
    }

    public Vector3dc center() {
        return center;
    }

    public double intersect(Ray ray) {
        scratch.set(center).sub(ray.origin());
        double tMin = 0;
        double tMax = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            double e = axes[i].dot(scratch);
            double f = axes[i].dot(ray.direction());
            if (Math.abs(f) > 1e-12) {
                double t1 = (e + half[i]) / f;
                double t2 = (e - half[i]) / f;
                tMin = Math.max(tMin, Math.min(t1, t2));
                tMax = Math.min(tMax, Math.max(t1, t2));
                if (tMin > tMax) return Double.NaN;
            } else if (Math.abs(e) > half[i]) {
                return Double.NaN;
            }
        }
        return tMin;
    }

    public boolean contains(Vector3dc p) {
        scratch.set(p).sub(center);
        for (int i = 0; i < 3; i++) if (Math.abs(axes[i].dot(scratch)) > half[i]) return false;
        return true;
    }
}
