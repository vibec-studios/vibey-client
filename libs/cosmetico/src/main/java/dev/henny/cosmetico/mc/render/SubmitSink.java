package dev.henny.cosmetico.mc.render;

//? if >=1.21.9 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.henny.cosmetico.mesh.MeshPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
//?}
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

//? if >=1.21.9 {
public final class SubmitSink extends MeshSink {
    private static final Geo[] RING = new Geo[4096];
    private static int next;
    private SubmitNodeCollector out;

    public SubmitSink set(SubmitNodeCollector out) {
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
    public void draw(PoseStack pose, RenderType type, MeshPart part, int light, int overlay, int argb) {
        out.submitCustomGeometry(pose, type, geo().set(part.data, part.quads, light, overlay, argb));
    }

    @Override
    public void quads(PoseStack pose, RenderType type, float[] data, int quads, int light, int argb) {
        out.submitCustomGeometry(pose, type, geo().set(data, quads, light, NO_OVERLAY, argb));
    }

    private static final class Geo implements SubmitNodeCollector.CustomGeometryRenderer {
        float[] data;
        int quads, light, overlay, argb;

        Geo set(float[] data, int quads, int light, int overlay, int argb) {
            this.data = data;
            this.quads = quads;
            this.light = light;
            this.overlay = overlay;
            this.argb = argb;
            return this;
        }

        @Override
        public void render(PoseStack.Pose pose, VertexConsumer vc) {
            VertexEmit.emit(pose, vc, data, quads, light, overlay, argb);
        }
    }
}
//?}
