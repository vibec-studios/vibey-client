package dev.henny.cosmetico.mc.render;

//? if <1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.mesh.MeshPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class BufferSink extends MeshSink {
    private MultiBufferSource buffers;

    public BufferSink set(MultiBufferSource buffers) {
        this.buffers = buffers;
        return this;
    }

    @Override
    public void draw(PoseStack pose, RenderType type, MeshPart part, int light, int overlay, int argb) {
        VertexEmit.emit(pose.last(), buffers.getBuffer(type), part.data, part.quads, light, overlay, argb);
        //? if =1.21.5
        /^flushEmissive(type);^/
    }

    @Override
    public void quads(PoseStack pose, RenderType type, float[] data, int quads, int light, int argb) {
        VertexEmit.emit(pose.last(), buffers.getBuffer(type), data, quads, light, NO_OVERLAY, argb);
        //? if =1.21.5
        /^flushEmissive(type);^/
    }

    //? if =1.21.5 {
    /^private void flushEmissive(RenderType type) {
        if (!EntityRenderTypes.isEmissive(type) || !(buffers instanceof MultiBufferSource.BufferSource bs)) return;
        net.minecraft.client.Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        bs.endBatch(type);
    }
    ^///?}
}
*///?}
