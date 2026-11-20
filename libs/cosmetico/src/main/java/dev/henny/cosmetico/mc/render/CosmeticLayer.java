package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.mc.compat.Errors;
import dev.henny.cosmetico.mc.compat.PoseCompat;
import dev.henny.cosmetico.state.Avatar;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//? if >=1.21.11 {
import net.minecraft.client.model.player.PlayerModel;
//?} else
/*import net.minecraft.client.model.PlayerModel;*/
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?} else {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

//? if >=1.21.9 {
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final SubmitSink SINK = new SubmitSink();
//?} else if >=1.21.2 {
/*public final class CosmeticLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private static final BufferSink SINK = new BufferSink();
*///?} else {
/*public final class CosmeticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final BufferSink SINK = new BufferSink();
*///?}
    private static final ModelPartFrames PARTS = new ModelPartFrames();
    private static final Errors ERRORS = new Errors("Cosmetic layer failed");

    //? if >=1.21.9 {
    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
        Avatar a = ((AvatarCarrier) state).cosmetico$avatar();
        if (a == null) return;
        draw(a, ps, SINK.set(out), light);
    }
    //?} else if >=1.21.2 {
    /*public CosmeticLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot) {
        Avatar a = ((AvatarCarrier) state).cosmetico$avatar();
        if (a == null) return;
        draw(a, ps, SINK.set(buffers), light);
    }
    *///?} else {
    /*public CosmeticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Avatar a = dev.henny.cosmetico.Cosmetico.engine().avatars.peek(player.getUUID());
        if (a == null) return;
        draw(a, ps, SINK.set(buffers), light);
    }
    *///?}

    private void draw(Avatar a, PoseStack ps, MeshSink sink, int light) {
        PoseStack.Pose saved = ps.last();
        try {
            RenderPasses.attached().draw(a, ps, sink, PARTS.set(getParentModel()), light);
        } catch (RuntimeException e) {
            PoseCompat.unwind(ps, saved);
            ERRORS.report(e);
        }
    }
}
