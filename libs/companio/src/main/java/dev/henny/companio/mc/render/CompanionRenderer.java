package dev.henny.companio.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.companio.mc.CompanioMod;
import dev.henny.companio.mc.client.CompanioClient;
import dev.henny.companio.move.Angles;
import dev.henny.companio.move.Body;
import dev.henny.companio.world.CompanionView;
import dev.henny.cosmetico.mc.compat.PoseCompat;
import dev.henny.cosmetico.mc.render.BoneDraw;
import dev.henny.cosmetico.mc.render.GpuBundle;
import dev.henny.cosmetico.mc.render.GpuCache;
import dev.henny.cosmetico.mc.render.SubmitSink;
import dev.henny.cosmetico.mc.render.WorldFrame;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.skeleton.Pass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class CompanionRenderer {
    private static final BoneDraw BONES = new BoneDraw();
    private static final SubmitSink SINK = new SubmitSink();
    private static final Mix MIX = new Mix();
    private static final ViewLocators LOCATORS = new ViewLocators();
    private static final Quaternionf Q = new Quaternionf();
    private static final double[] POS = new double[4];
    private static boolean[] hidden = new boolean[64];
    private static int errors;

    private CompanionRenderer() {}

    public static void submit(PoseStack pose, LevelRenderState state, SubmitNodeCollector out) {
        CompanioClient client = CompanioClient.get();
        Minecraft mc = Minecraft.getInstance();
        if (client == null || mc.level == null || state.cameraRenderState == null) return;
        PoseStack ps = pose == null ? new PoseStack() : pose;
        PoseStack.Pose saved = ps.last();
        try {
            Vec3 cam = state.cameraRenderState.pos;
            float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            SINK.set(out);
            for (CompanionView v : client.views()) draw(client, v, ps, cam, pt, mc);
        } catch (RuntimeException e) {
            PoseCompat.unwind(ps, saved);
            if (errors++ < 3) CompanioMod.LOG.error("Companion render failed", e);
        }
    }

    private static void draw(CompanioClient client, CompanionView v, PoseStack ps, Vec3 cam, float pt, Minecraft mc) {
        Bundle b = v.model();
        if (b == null || b.model == null) return;
        GpuBundle gb = GpuCache.get(b);
        if (gb == null) return;
        position(client, v, pt, POS);
        double x = POS[0], y = POS[1], z = POS[2];
        float yaw = (float) POS[3];
        float s = v.def().scale() * v.size();
        ps.pushPose();
        ps.translate((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z));
        PoseCompat.rotate(ps, Q.rotationY((float) Math.toRadians(180f - yaw)));
        ps.scale(s, s, s);
        WorldFrame.toModelSpace(ps);
        v.animator().fill(MIX);
        hidden = v.hiddenBones(hidden);
        MIX.hidden = hidden;
        if (v.def().lookBone() >= 0) MIX.rotate(v.def().lookBone(), -v.headPitch(pt), -v.headYaw(pt), 0f);
        int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(x, y + 0.3, z));
        BONES.draw(gb, MIX, ps, SINK, null, Pass.WORLD, light, -1, LOCATORS.begin(v, cam.x, cam.y, cam.z));
        ps.popPose();
    }

    public static void position(CompanioClient client, CompanionView v, float pt, double[] out) {
        if (client.ridingPosition(v, pt, out)) return;
        Body b = v.body();
        out[0] = b.px + (b.x - b.px) * pt;
        out[1] = b.py + (b.y - b.py) * pt;
        out[2] = b.pz + (b.z - b.pz) * pt;
        out[3] = Angles.lerp(pt, b.pyaw, b.yaw);
    }
}
