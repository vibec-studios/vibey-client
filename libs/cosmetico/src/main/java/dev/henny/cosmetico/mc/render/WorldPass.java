package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.mc.compat.Errors;
import dev.henny.cosmetico.mc.compat.PoseCompat;
import dev.henny.cosmetico.mc.fx.SpriteRender;
import dev.henny.cosmetico.state.Avatar;
import org.joml.Quaternionf;

public final class WorldPass {
    private WorldPass() {}

    private static final Errors ERRORS = new Errors("Cosmetic world pass failed");
    //? if >=1.21.9 {
    private static final SubmitSink SUBMITS = new SubmitSink();
    //?}
    //? if <1.21.9
    /*private static final BufferSink BUFFERS = new BufferSink();*/
    private static final Quaternionf CAM = new Quaternionf();

    //? if >=1.21.9 {
    public static void submit(Avatar a, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack ps,
                              net.minecraft.client.renderer.SubmitNodeCollector out,
                              //? if >=26.1 {
                              /*net.minecraft.client.renderer.state.level.CameraRenderState camera) {
                              *///?} else
                              net.minecraft.client.renderer.state.CameraRenderState camera) {
        draw(a, ps, SUBMITS.set(out), state.lightCoords, state.x, state.y, state.z, state.scale,
                camera.pos.x, camera.pos.y, camera.pos.z, camera.orientation);
    }
    //?}

    //? if >=1.21.2 <1.21.9 {
    /*public static void render(Avatar a, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack ps,
                              net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        //? if >=1.21.6 {
        /^net.minecraft.world.phys.Vec3 cp = cam.position();
        ^///?} else
        /^net.minecraft.world.phys.Vec3 cp = cam.getPosition();^/
        draw(a, ps, BUFFERS.set(buffers), light, state.x, state.y, state.z, state.scale, cp.x, cp.y, cp.z, cam.rotation());
    }
    *///?}
    //? if <1.21.2 {
    /*public static void render121(net.minecraft.client.player.AbstractClientPlayer player, float partial, PoseStack ps,
                                 net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        Avatar a = dev.henny.cosmetico.mc.player.PlayerSnapshot.extract(player, partial);
        if (a == null) return;
        net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        net.minecraft.world.phys.Vec3 cp = cam.getPosition();
        draw(a, ps, BUFFERS.set(buffers), light, a.frame.x, a.frame.y, a.frame.z, player.getScale(), cp.x, cp.y, cp.z, cam.rotation());
    }
    *///?}

    public static void draw(Avatar a, PoseStack ps, MeshSink sink, int light, double x, double y, double z, float scale,
                            double camX, double camY, double camZ, Quaternionf camRot) {
        PoseStack.Pose saved = ps.last();
        try {
            if (GuiDepth.inGui() && !a.preview) return;
            if (a.preview) camX = Double.NaN;
            RenderPasses.locators().camera(camX, camY, camZ);
            ps.pushPose();
            if (a.frame.crouching) ps.translate(0f, 2f / 16f * scale, 0f);
            RenderPasses.world().draw(a, ps, sink, light, x, y, z, 0.9375f * scale);
            ps.popPose();
            if (!a.preview) SpriteRender.render(a, ps, sink, x, y, z, CAM.set(camRot));
        } catch (RuntimeException e) {
            PoseCompat.unwind(ps, saved);
            ERRORS.report(e);
        }
    }
}
