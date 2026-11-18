package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.fx.SpriteParticles;
import org.joml.Quaternionf;

/**
 * Called at the start of the player's render with the pose stack at the player's feet (camera relative, world
 * axes): world-attached cosmetics (pets, auras) and the avatar's sprite particles.
 */
public final class WorldPass {
    private WorldPass() {}

    private static int errors;
    /** > 0 while vanilla draws an entity into the GUI (inventory): world-space things are skipped there. */
    public static int gui;
    //? if >=1.21.9 {
    private static final MeshSink.Submits SUBMITS = new MeshSink.Submits();
    //?}
    //? if <1.21.9
    /*private static final MeshSink.Buffers BUFFERS = new MeshSink.Buffers();*/
    private static final Quaternionf CAM = new Quaternionf();

    //? if >=1.21.9 {
    public static void submit(AvatarState st, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack ps,
                              net.minecraft.client.renderer.SubmitNodeCollector out,
                              //? if >=26.1 {
                              /*net.minecraft.client.renderer.state.level.CameraRenderState camera) {
                              *///?} else
                              net.minecraft.client.renderer.state.CameraRenderState camera) {
        draw(st, ps, SUBMITS.set(out), state.lightCoords, state.x, state.y, state.z, state.scale,
                camera.pos.x, camera.pos.y, camera.pos.z, camera.orientation);
    }
    //?}

    //? if >=1.21.2 <1.21.9 {
    /*public static void render(AvatarState st, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack ps,
                              net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        //? if >=1.21.6 {
        /^net.minecraft.world.phys.Vec3 cp = cam.position();
        ^///?} else
        /^net.minecraft.world.phys.Vec3 cp = cam.getPosition();^/
        draw(st, ps, BUFFERS.set(buffers), light, state.x, state.y, state.z, state.scale, cp.x, cp.y, cp.z, cam.rotation());
    }
    *///?}
    //? if <1.21.2 {
    /*public static void render121(net.minecraft.client.player.AbstractClientPlayer player, float partial, PoseStack ps,
                                 net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        AvatarState st = AvatarHooks.extract(player, partial);
        if (st == null) return;
        net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        net.minecraft.world.phys.Vec3 cp = cam.getPosition();
        draw(st, ps, BUFFERS.set(buffers), light, st.x, st.y, st.z, player.getScale(), cp.x, cp.y, cp.z, cam.rotation());
    }
    *///?}

    private static void draw(AvatarState st, PoseStack ps, MeshSink sink, int light, double x, double y, double z, float scale,
                             double camX, double camY, double camZ, Quaternionf camRot) {
        PoseStack.Pose saved = ps.last();
        try {
            if (gui > 0 && !st.preview) return;
            if (st.preview) camX = Double.NaN; // GUI previews: no locator world positions
            AvatarDraw.camera(camX, camY, camZ);
            ps.pushPose();
            // the renderer moved crouching players down by 2 px; the world frame stays at the real feet
            if (st.crouching) ps.translate(0f, 2f / 16f * scale, 0f);
            AvatarDraw.world(st, ps, sink, light, x, y, z, 0.9375f * scale);
            ps.popPose();
            if (!st.preview) SpriteParticles.render(st, ps, sink, x, y, z, CAM.set(camRot));
        } catch (RuntimeException e) {
            PoseCompat.unwind(ps, saved);
            if (errors++ < 3) dev.henny.vibeyclient.Vibey.LOG.error("Cosmetic world pass failed", e);
        }
    }
}
