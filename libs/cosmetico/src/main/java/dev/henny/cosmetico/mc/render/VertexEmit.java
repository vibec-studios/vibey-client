package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.henny.cosmetico.mesh.BundleMesh;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class VertexEmit {
    private VertexEmit() {}

    private static final Vector3f P = new Vector3f();
    private static final Vector3f N = new Vector3f();

    static void emit(PoseStack.Pose pose, VertexConsumer vc, float[] d, int quads, int light, int overlay, int argb) {
        Matrix4f m = pose.pose();
        Matrix3f nm = pose.normal();
        int n = quads * 4 * BundleMesh.STRIDE;
        for (int i = 0; i < n; i += BundleMesh.STRIDE) {
            m.transformPosition(d[i], d[i + 1], d[i + 2], P);
            nm.transform(d[i + 5], d[i + 6], d[i + 7], N);
            float len = N.lengthSquared();
            if (len > 1e-8f && Math.abs(len - 1f) > 1e-4f) N.mul((float) (1.0 / Math.sqrt(len)));
            vc.addVertex(P.x, P.y, P.z, argb, d[i + 3], d[i + 4], overlay, light, N.x, N.y, N.z);
        }
    }
}
