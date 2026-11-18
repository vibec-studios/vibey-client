package dev.henny.vibeyclient.avatar.preview;

//? if <1.21.6 {
/*import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.PlayerSkin;
import org.joml.Quaternionf;

/^*
 * GUI preview before 1.21.6 (no picture-in-picture renderers): drawn straight into the GUI like vanilla's inventory
 * player. 1.21.2 - 1.21.5 go through the vanilla player renderer with a synthetic render state; 1.21.1 has no render
 * states, so the player model, cape and cosmetics are drawn here directly.
 ^/
final class PreviewLegacy {
    private PreviewLegacy() {}

    //? if >=1.21.2 {
    /^static void render(Gfx g, int x, int y, int w, int h, float size, net.minecraft.client.renderer.entity.state.PlayerRenderState rs, PlayerSkin skin) {
        GuiGraphics gg = g.g;
        PoseStack ps = gg.pose();
        Minecraft mc = Minecraft.getInstance();
        @SuppressWarnings({"rawtypes", "unchecked"})
        java.util.Map<Object, net.minecraft.client.renderer.entity.EntityRenderer> map =
                ((dev.henny.vibeyclient.module.cosmetics.mixin.EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).vibey$playerRenderers();
        @SuppressWarnings("rawtypes")
        net.minecraft.client.renderer.entity.EntityRenderer r = map.get(skin.model());
        if (r == null) return;
        gg.enableScissor(x, y, x + w, y + h);
        ps.pushPose();
        ps.translate(x + w / 2f, y + h / 2f, 50.0);
        ps.scale(size, size, -size);
        ps.translate(0f, 0.96f, 0f);
        ps.mulPose(new Quaternionf().rotateZ((float) Math.PI));
        gg.flush();
        Lighting.setupForEntityInInventory();
        @SuppressWarnings("unchecked")
        java.util.function.Consumer<net.minecraft.client.renderer.MultiBufferSource> draw = buffers -> r.render(rs, ps, buffers, 0xF000F0);
        gg.drawSpecial(draw);
        gg.flush();
        ps.popPose();
        Lighting.setupFor3DItems();
        gg.disableScissor();
    }
    ^///?} else {
    private static net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> wide, slim;
    private static final dev.henny.vibeyclient.avatar.render.MeshSink.Buffers SINK = new dev.henny.vibeyclient.avatar.render.MeshSink.Buffers();
    private static final dev.henny.vibeyclient.avatar.render.ModelParts PARTS = new dev.henny.vibeyclient.avatar.render.ModelParts();

    static void render(Gfx g, int x, int y, int w, int h, float size, float yaw, AvatarState st, PlayerSkin skin, double now) {
        Minecraft mc = Minecraft.getInstance();
        boolean isSlim = skin.model() == PlayerSkin.Model.SLIM;
        if (wide == null) {
            wide = new net.minecraft.client.model.PlayerModel<>(mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
            slim = new net.minecraft.client.model.PlayerModel<>(mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM), true);
        }
        net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> m = isSlim ? slim : wide;
        m.young = false;
        m.riding = false;
        m.crouching = false;
        m.attackTime = 0f;
        for (net.minecraft.client.model.geom.ModelPart p : new net.minecraft.client.model.geom.ModelPart[]{m.head, m.hat, m.body, m.jacket,
                m.rightArm, m.leftArm, m.rightSleeve, m.leftSleeve, m.rightLeg, m.leftLeg, m.rightPants, m.leftPants}) p.resetPose();
        float age = (float) (now * 20.0);
        m.rightArm.zRot += net.minecraft.util.Mth.cos(age * 0.09f) * 0.05f + 0.05f;
        m.leftArm.zRot -= net.minecraft.util.Mth.cos(age * 0.09f) * 0.05f + 0.05f;
        m.rightArm.xRot += net.minecraft.util.Mth.sin(age * 0.067f) * 0.05f;
        m.leftArm.xRot -= net.minecraft.util.Mth.sin(age * 0.067f) * 0.05f;
        dev.henny.vibeyclient.avatar.render.AvatarHooks.emotePose(m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg, st);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);

        GuiGraphics gg = g.g;
        PoseStack ps = gg.pose();
        gg.enableScissor(x, y, x + w, y + h);
        ps.pushPose();
        ps.translate(x + w / 2f, y + h / 2f, 50.0);
        ps.scale(size, size, -size);
        ps.translate(0f, 0.96f, 0f);
        ps.mulPose(new Quaternionf().rotateZ((float) Math.PI));
        gg.flush();
        Lighting.setupForEntityInInventory();
        net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers = gg.bufferSource();
        int light = 0xF000F0;
        // world-attached cosmetics (pet) at the feet, world axes
        dev.henny.vibeyclient.avatar.render.AvatarDraw.world(st, ps, SINK.set(buffers), light, 0, 0, 0, 0.9375f);
        // the player: like LivingEntityRenderer.render + setupRotations
        ps.pushPose();
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180f - (180f + yaw))));
        dev.henny.vibeyclient.avatar.render.AvatarHooks.rootTransform(st, ps, 0.9375f);
        ps.scale(-1f, -1f, 1f);
        ps.scale(0.9375f, 0.9375f, 0.9375f);
        ps.translate(0f, -1.501f, 0f);
        m.renderToBuffer(ps, buffers.getBuffer(m.renderType(skin.texture())), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        if (skin.capeTexture() != null) {
            ps.pushPose();
            ps.translate(0f, 0f, 0.125f);
            ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(6f));
            ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
            m.renderCloak(ps, buffers.getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(skin.capeTexture())), light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        dev.henny.vibeyclient.avatar.render.AvatarDraw.attached(st, ps, SINK, PARTS.set(m), light);
        ps.popPose();
        gg.flush();
        ps.popPose();
        Lighting.setupFor3DItems();
        gg.disableScissor();
    }
    //?}
}
*///?}
