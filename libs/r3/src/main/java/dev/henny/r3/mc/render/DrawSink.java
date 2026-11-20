package dev.henny.r3.mc.render;

//? if >=1.21.9 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}

final class DrawSink {
    static final int FIRST_ORDER = 1;

    private DrawSink() {}

    //? if >=1.21.9 {
    static void submit(DrawList list, SubmitNodeCollector out) {
        PoseStack identity = new PoseStack();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            var ordered = out.order(FIRST_ORDER + i);
            if (item instanceof QuadItem q) {
                if (q.vertices < 4) continue;
                ordered.submitCustomGeometry(identity, RenderTypeCompat.of(q.mode, q.texture), (pose, vc) -> VertexEmitter.emit(q, vc));
            } else if (item instanceof TextItem t) {
                FontDraw.submit(ordered, t);
            }
        }
    }
    //?} else {
    /*static void draw(DrawList list, MultiBufferSource buffers) {
        MultiBufferSource.BufferSource batch = buffers instanceof MultiBufferSource.BufferSource b ? b : null;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof QuadItem q) {
                if (q.vertices < 4) continue;
                var type = RenderTypeCompat.of(q.mode, q.texture);
                VertexEmitter.emit(q, buffers.getBuffer(type));
                if (batch != null) batch.endBatch(type);
            } else if (item instanceof TextItem t) {
                FontDraw.draw(buffers, t);
                if (batch != null) batch.endBatch();
            }
        }
    }
    *///?}
}
