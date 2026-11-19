package dev.henny.r3.view;

import dev.henny.r3.math.Ray;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;
import org.joml.Vector4f;

public final class PointerRays {
    private static final ThreadLocal<Matrix4f> INVERSE = ThreadLocal.withInitial(Matrix4f::new);
    private static final ThreadLocal<Vector4f> NEAR = ThreadLocal.withInitial(Vector4f::new);
    private static final ThreadLocal<Vector4f> FAR = ThreadLocal.withInitial(Vector4f::new);

    private PointerRays() {}

    public static Ray fromCrosshair(CameraState camera, Ray out) {
        return out.set(camera.position(), camera.look());
    }

    public static Ray fromScreen(double mouseX, double mouseY, double screenWidth, double screenHeight,
                                 Matrix4fc projection, Matrix4fc view, Vector3dc cameraPosition, Ray out) {
        float ndcX = (float) (2.0 * mouseX / screenWidth - 1.0);
        float ndcY = (float) (1.0 - 2.0 * mouseY / screenHeight);
        Matrix4f inverse = INVERSE.get().set(projection).mul(view).invert();
        Vector4f near = inverse.transform(NEAR.get().set(ndcX, ndcY, -1f, 1f));
        Vector4f far = inverse.transform(FAR.get().set(ndcX, ndcY, 1f, 1f));
        near.div(near.w);
        far.div(far.w);
        return out.set(cameraPosition.x() + near.x, cameraPosition.y() + near.y, cameraPosition.z() + near.z,
                far.x - near.x, far.y - near.y, far.z - near.z);
    }

    public static Ray fromScreenPoint(double x, double y, Ray out) {
        return out.set(x, y, -1.0, 0, 0, 1);
    }
}
