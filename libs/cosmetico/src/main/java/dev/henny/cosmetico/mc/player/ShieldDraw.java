package dev.henny.cosmetico.mc.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.henny.cosmetico.mc.compat.Errors;
import dev.henny.cosmetico.mc.render.Tex;
import dev.henny.cosmetico.state.Avatar;
import net.minecraft.client.model.geom.ModelPart;

public final class ShieldDraw {
    private ShieldDraw() {}

    public static Avatar owner;
    private static final Errors ERRORS = new Errors("Shield cosmetic failed");

    public static Tex texture() {
        try {
            return owner == null ? null : CapeShieldTextures.shield(owner);
        } catch (RuntimeException e) {
            ERRORS.report(e);
            return null;
        }
    }

    public static void draw(ModelPart handle, ModelPart plate, PoseStack ps, Object out, Tex tex, int light, int overlay) {
        ps.pushPose();
        ps.scale(1f, -1f, -1f);
        //? if >=1.21.9 {
        Geo g = GEO[next];
        if (g == null) GEO[next] = g = new Geo();
        next = (next + 1) & (GEO.length - 1);
        g.handle = handle;
        g.plate = plate;
        g.light = light;
        g.overlay = overlay;
        ((net.minecraft.client.renderer.SubmitNodeCollector) out).submitCustomGeometry(ps, tex.cutout(), g);
        //?} else {
        /*VertexConsumer vc = ((net.minecraft.client.renderer.MultiBufferSource) out).getBuffer(tex.cutout());
        handle.render(ps, vc, light, overlay);
        plate.render(ps, vc, light, overlay);
        *///?}
        ps.popPose();
    }

    //? if >=1.21.9 {
    private static final Geo[] GEO = new Geo[64];
    private static int next;
    private static final PoseStack TMP = new PoseStack();

    private static final class Geo implements net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer {
        ModelPart handle, plate;
        int light, overlay;

        @Override
        public void render(PoseStack.Pose pose, VertexConsumer vc) {
            TMP.last().pose().set(pose.pose());
            TMP.last().normal().set(pose.normal());
            handle.render(TMP, vc, light, overlay);
            plate.render(TMP, vc, light, overlay);
        }
    }
    //?}
}
