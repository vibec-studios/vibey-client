package dev.henny.cosmetico.mc.preview;

//? if <1.21.6 {
/*import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.PlayerSkin;
import org.joml.Quaternionf;

final class PreviewLegacy {
    private PreviewLegacy() {}

    //? if >=1.21.2 {
    /^static void render(GuiGraphics gg, int x, int y, int w, int h, float size, net.minecraft.client.renderer.entity.state.PlayerRenderState rs, PlayerSkin skin) {
        PoseStack ps = gg.pose();
        Minecraft mc = Minecraft.getInstance();
        @SuppressWarnings({"rawtypes", "unchecked"})
        java.util.Map<Object, net.minecraft.client.renderer.entity.EntityRenderer> map =
                ((dev.henny.cosmetico.mc.mixin.EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher()).cosmetico$playerRenderers();
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
    private static final dev.henny.cosmetico.mc.render.BufferSink SINK = new dev.henny.cosmetico.mc.render.BufferSink();
    private static final dev.henny.cosmetico.mc.render.ModelPartFrames PARTS = new dev.henny.cosmetico.mc.render.ModelPartFrames();

    static void render(GuiGraphics gg, int x, int y, int w, int h, float size, float yaw, dev.henny.cosmetico.state.Avatar st, PlayerSkin skin, double now) {
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
        dev.henny.cosmetico.mc.player.EmotePoseHook.apply(m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg, st);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);

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
        dev.henny.cosmetico.mc.render.RenderPasses.world().draw(st, ps, SINK.set(buffers), light, 0, 0, 0, 0.9375f);
        ps.pushPose();
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180f - (180f + yaw))));
        dev.henny.cosmetico.mc.player.RootPoseHook.apply(st, ps, 0.9375f);
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
        dev.henny.cosmetico.mc.render.RenderPasses.attached().draw(st, ps, SINK, PARTS.set(m), light);
        ps.popPose();
        gg.flush();
        ps.popPose();
        Lighting.setupFor3DItems();
        gg.disableScissor();
    }
    //?}
}
*///?}
