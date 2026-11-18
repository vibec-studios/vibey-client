package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarState;
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

/**
 * The player render layer that draws Vibey cosmetics and emote props on the posed player model. Added to the
 * player renderer by a mixin; never throws into the renderer.
 */
//? if >=1.21.9 {
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final MeshSink.Submits SINK = new MeshSink.Submits();
//?} else if >=1.21.2 {
/*public final class CosmeticLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private static final MeshSink.Buffers SINK = new MeshSink.Buffers();
*///?} else {
/*public final class CosmeticLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final MeshSink.Buffers SINK = new MeshSink.Buffers();
*///?}
    private static final ModelParts PARTS = new ModelParts();
    private static int errors;

    //? if >=1.21.9 {
    public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
        AvatarState st = ((AvatarCarrier) state).vibey$avatar();
        if (st == null) return;
        draw(st, ps, SINK.set(out), light);
    }
    //?} else if >=1.21.2 {
    /*public CosmeticLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot) {
        AvatarState st = ((AvatarCarrier) state).vibey$avatar();
        if (st == null) return;
        draw(st, ps, SINK.set(buffers), light);
    }
    *///?} else {
    /*public CosmeticLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        AvatarState st = dev.henny.vibeyclient.avatar.AvatarStates.peek(player.getUUID());
        if (st == null) return;
        draw(st, ps, SINK.set(buffers), light);
    }
    *///?}

    private void draw(AvatarState st, PoseStack ps, MeshSink sink, int light) {
        try {
            AvatarDraw.attached(st, ps, sink, PARTS.set(getParentModel()), light);
        } catch (RuntimeException e) {
            if (errors++ < 3) dev.henny.vibeyclient.Vibey.LOG.error("Cosmetic layer failed", e);
        }
    }
}
