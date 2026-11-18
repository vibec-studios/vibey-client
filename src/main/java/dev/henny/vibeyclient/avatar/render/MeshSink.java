package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <1.21.9
/*import net.minecraft.client.renderer.MultiBufferSource;*/
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/
//? if >=1.21.9
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Where the engine's quads go. Up to 1.21.8 straight into a buffer source; from 1.21.9 the game collects submits for
 * the frame and draws later, so quads are handed over as custom geometry (the pose is copied at submit time). Render
 * thread only; no allocation per frame (the submit callbacks come from a ring of reusable objects).
 */
public abstract class MeshSink {
    public static final int FULL_BRIGHT = 0xF000F0;
    public static final int NO_OVERLAY = 655360;

    /** Draws {@code part} with the pose on top of {@code pose}. */
    public abstract void draw(PoseStack pose, RenderType type, BundleMesh.Part part, int light, int overlay, int argb);

    /** A camera-facing or arbitrary quad list (sprite particles): {@code data} as in {@link BundleMesh.Part}. */
    public abstract void quads(PoseStack pose, RenderType type, float[] data, int quads, int light, int argb);

    // ============================================================================================ emit

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

    //? if <1.21.9 {
    /*// ============================================================================================ buffer source

    public static final class Buffers extends MeshSink {
        private MultiBufferSource buffers;

        public Buffers set(MultiBufferSource buffers) {
            this.buffers = buffers;
            return this;
        }

        @Override
        public void draw(PoseStack pose, RenderType type, BundleMesh.Part part, int light, int overlay, int argb) {
            emit(pose.last(), buffers.getBuffer(type), part.data, part.quads, light, overlay, argb);
            //? if =1.21.5
            /^flushEmissive(type);^/
        }

        @Override
        public void quads(PoseStack pose, RenderType type, float[] data, int quads, int light, int argb) {
            emit(pose.last(), buffers.getBuffer(type), data, quads, light, NO_OVERLAY, argb);
            //? if =1.21.5
            /^flushEmissive(type);^/
        }

        //? if =1.21.5 {
        /^// 1.21.5: vanilla's entity_translucent_emissive pipeline samples the lightmap (Sampler2) but its render type
        // never binds it, so drawing the batch later (whenever the shared buffer flushes) throws "Missing sampler
        // Sampler2" once another render type turned the light layer off. Draw it right away with the lightmap bound.
        private void flushEmissive(RenderType type) {
            if (!Tex.isEmissive(type) || !(buffers instanceof MultiBufferSource.BufferSource bs)) return;
            net.minecraft.client.Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            bs.endBatch(type);
        }
        ^///?}
    }
    *///?}

    //? if >=1.21.9 {
    // ============================================================================================ submit collector

    public static final class Submits extends MeshSink {
        private static final Geo[] RING = new Geo[4096];
        private static int next;
        private SubmitNodeCollector out;

        public Submits set(SubmitNodeCollector out) {
            this.out = out;
            return this;
        }

        private static Geo geo() {
            Geo g = RING[next];
            if (g == null) RING[next] = g = new Geo();
            next = (next + 1) & (RING.length - 1);
            return g;
        }

        @Override
        public void draw(PoseStack pose, RenderType type, BundleMesh.Part part, int light, int overlay, int argb) {
            Geo g = geo();
            g.data = part.data;
            g.quads = part.quads;
            g.light = light;
            g.overlay = overlay;
            g.argb = argb;
            out.submitCustomGeometry(pose, type, g);
        }

        @Override
        public void quads(PoseStack pose, RenderType type, float[] data, int quads, int light, int argb) {
            Geo g = geo();
            g.data = data;
            g.quads = quads;
            g.light = light;
            g.overlay = NO_OVERLAY;
            g.argb = argb;
            out.submitCustomGeometry(pose, type, g);
        }

        private static final class Geo implements SubmitNodeCollector.CustomGeometryRenderer {
            float[] data;
            int quads, light, overlay, argb;

            @Override
            public void render(PoseStack.Pose pose, VertexConsumer vc) {
                emit(pose, vc, data, quads, light, overlay, argb);
            }
        }
    }
    //?}
}
