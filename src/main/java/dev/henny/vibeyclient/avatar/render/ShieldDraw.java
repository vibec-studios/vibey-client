package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.henny.vibeyclient.avatar.AvatarState;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Shield cosmetics: while the held items of a player are drawn ({@link #owner} set by the item-in-hand hooks),
 * vanilla's shield draw is replaced by the shield model with the cosmetic's 64x64 texture (banner patterns are not
 * drawn on top). Render thread only.
 */
public final class ShieldDraw {
    private ShieldDraw() {}

    /** The avatar whose held items are being drawn right now, or null. */
    public static AvatarState owner;
    private static int errors;

    /** The texture to use instead of vanilla's, or null for vanilla. */
    public static Tex texture() {
        try {
            return owner == null ? null : AvatarHooks.shieldTexture(owner);
        } catch (RuntimeException e) {
            if (errors++ < 3) dev.henny.vibeyclient.Vibey.LOG.error("Shield cosmetic failed", e);
            return null;
        }
    }

    /** Pose stack as vanilla's shield renderer gets it (before its scale(1, -1, -1)). */
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
