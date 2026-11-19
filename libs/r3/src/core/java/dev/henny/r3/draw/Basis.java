package dev.henny.r3.draw;

import dev.henny.r3.object.Transform;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

public final class Basis {
    private final Vector3d scratch = new Vector3d();
    public final Vector3f center = new Vector3f();
    public final Vector3f right = new Vector3f();
    public final Vector3f up = new Vector3f();
    public final Vector3f normal = new Vector3f();

    public Basis set(Transform t, Vector3dc camera, double normalOffset) {
        t.right(scratch);
        right.set((float) scratch.x, (float) scratch.y, (float) scratch.z);
        t.up(scratch);
        up.set((float) scratch.x, (float) scratch.y, (float) scratch.z);
        t.normal(scratch);
        normal.set((float) scratch.x, (float) scratch.y, (float) scratch.z);
        Vector3dc p = t.position();
        center.set((float) (p.x() - camera.x() + scratch.x * normalOffset),
                (float) (p.y() - camera.y() + scratch.y * normalOffset),
                (float) (p.z() - camera.z() + scratch.z * normalOffset));
        return this;
    }

    public Basis offsetInPlane(float dx, float dy) {
        center.add(right.x * dx + up.x * dy, right.y * dx + up.y * dy, right.z * dx + up.z * dy);
        return this;
    }
}
