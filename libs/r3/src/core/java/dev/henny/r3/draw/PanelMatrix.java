package dev.henny.r3.draw;

import dev.henny.r3.object.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class PanelMatrix {
    private final Vector3d right = new Vector3d();
    private final Vector3d up = new Vector3d();
    private final Vector3d normal = new Vector3d();

    public Matrix4f compute(Transform t, int pixelWidth, int pixelHeight, Vector3dc camera, Matrix4f out) {
        t.right(right);
        t.up(up);
        t.normal(normal);
        double w = t.worldWidth();
        double h = t.worldHeight();
        double sx = w / pixelWidth;
        double sy = h / pixelHeight;
        Vector3dc c = t.position();
        double ox = c.x() - camera.x() - right.x * w / 2 + up.x * h / 2;
        double oy = c.y() - camera.y() - right.y * w / 2 + up.y * h / 2;
        double oz = c.z() - camera.z() - right.z * w / 2 + up.z * h / 2;
        return out.set(
                (float) (right.x * sx), (float) (right.y * sx), (float) (right.z * sx), 0f,
                (float) (-up.x * sy), (float) (-up.y * sy), (float) (-up.z * sy), 0f,
                (float) (normal.x * sx), (float) (normal.y * sx), (float) (normal.z * sx), 0f,
                (float) ox, (float) oy, (float) oz, 1f);
    }
}
