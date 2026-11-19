package dev.henny.r3.math;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

public final class Axes {
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private static final class Scratch {
        final Vector3f axis = new Vector3f();
        final Matrix3f matrix = new Matrix3f();
        final Vector3d normal = new Vector3d();
        final Vector3d right = new Vector3d();
        final Vector3d up = new Vector3d();
    }

    private Axes() {}

    public static Vector3d right(Quaternionfc q, Vector3d out) {
        return axis(q, 1, 0, 0, out);
    }

    public static Vector3d up(Quaternionfc q, Vector3d out) {
        return axis(q, 0, 1, 0, out);
    }

    public static Vector3d normal(Quaternionfc q, Vector3d out) {
        return axis(q, 0, 0, 1, out);
    }

    private static Vector3d axis(Quaternionfc q, float x, float y, float z, Vector3d out) {
        Vector3f v = q.transform(SCRATCH.get().axis.set(x, y, z));
        return out.set(v.x, v.y, v.z);
    }

    public static Quaternionf fromAxes(Vector3dc right, Vector3dc up, Vector3dc normal, Quaternionf out) {
        Matrix3f m = SCRATCH.get().matrix.set(
                (float) right.x(), (float) right.y(), (float) right.z(),
                (float) up.x(), (float) up.y(), (float) up.z(),
                (float) normal.x(), (float) normal.y(), (float) normal.z());
        return out.setFromNormalized(m).normalize();
    }

    public static Quaternionf facing(double nx, double ny, double nz, Quaternionf out) {
        Scratch s = SCRATCH.get();
        Vector3d n = s.normal.set(nx, ny, nz);
        if (n.lengthSquared() < 1e-12) return out.identity();
        n.normalize();
        Vector3d right = s.right.set(0, 1, 0).cross(n);
        if (right.lengthSquared() < 1e-8) right.set(1, 0, 0);
        right.normalize();
        Vector3d up = s.up.set(n).cross(right).normalize();
        return fromAxes(right, up, n, out);
    }
}
