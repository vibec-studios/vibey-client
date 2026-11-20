package dev.henny.r3.mc.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

final class VertexEmitter {
    private static final int NO_OVERLAY = 0xA0000;

    private VertexEmitter() {}

    static void emit(QuadItem q, VertexConsumer out) {
        float[] f = q.xyzuv;
        int[] cl = q.colorLight;
        int count = q.vertices - q.vertices % 4;
        for (int v = 0; v < count; v++) {
            int i = v * QuadItem.FLOATS;
            out.addVertex(f[i], f[i + 1], f[i + 2])
                    .setColor(cl[v * 2])
                    .setUv(f[i + 3], f[i + 4])
                    .setOverlay(NO_OVERLAY)
                    .setLight(cl[v * 2 + 1])
                    .setNormal(0f, 1f, 0f);
        }
    }
}
