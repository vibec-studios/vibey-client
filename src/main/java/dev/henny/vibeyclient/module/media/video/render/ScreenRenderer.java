package dev.henny.vibeyclient.module.media.video.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the screens as quads in the world (submitted for this frame, 1.21.11 render API): a dark bezel/back plate,
 * the picture on top ("text" render type: textured, lightmapped, no shading) and, without a picture, a status line.
 * Glowing screens are full bright, others take the light in front of them.
 */
public final class ScreenRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float BEZEL = 0.04f;
    private static final int BEZEL_COLOR = 0xFF0E0E12;
    private static final int PANEL_COLOR = 0xFF16161C;
    private static final double MAX_DISTANCE = 128;

    private ScreenRenderer() {}

    public static void render(Video video, PoseStack pose, SubmitNodeCollector out) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 cp = cam.position();
        String dim = video.dimension();
        for (MediaScreen s : video.screens.values()) {
            if (!s.placed() || !dim.equals(s.dimension)) continue;
            if (s.center().distanceToSqr(cp) > MAX_DISTANCE * MAX_DISTANCE) continue;
            draw(video, s, pose, out, cp, mc, false);
        }
        MediaScreen draft = video.placement.draft();
        if (draft != null && video.placement.valid) draw(video, draft, pose, out, cp, mc, true);
    }

    private static void draw(Video video, MediaScreen s, PoseStack pose, SubmitNodeCollector out, Vec3 cp, Minecraft mc, boolean ghost) {
        Vec3 c = s.center().subtract(cp);
        Vec3 r = s.right();
        Vec3 u = s.upVec();
        Vec3 n = new Vec3(s.normal.getStepX(), s.normal.getStepY(), s.normal.getStepZ());
        int light = s.glowing || ghost ? FULL_BRIGHT
                : LevelRenderer.getLightColor(mc.level, BlockPos.containing(s.center().add(n.scale(0.3))));
        float hw = s.width / 2f;
        float hh = s.height / 2f;

        // Bezel + back (both windings so it hides the picture from behind).
        Vec3 back = c.subtract(n.scale(0.012));
        int plate = ghost ? 0x804C3BFF : BEZEL_COLOR;
        quadColor(pose, out, back, r, u, hw + BEZEL, hh + BEZEL, plate, light);

        Identifier tex = ghost || s.player == null || s.player.audioOnly() ? null : s.player.texture();
        if (tex != null) {
            final Vec3 cc = c;
            RenderType type = RenderTypes.text(tex);
            out.submitCustomGeometry(pose, type, (p, vc) -> {
                Matrix4f m = p.pose();
                vertex(vc, m, cc, r, u, -hw, hh, 0f, 0f, light);
                vertex(vc, m, cc, r, u, -hw, -hh, 0f, 1f, light);
                vertex(vc, m, cc, r, u, hw, -hh, 1f, 1f, light);
                vertex(vc, m, cc, r, u, hw, hh, 1f, 0f, light);
            });
            return;
        }
        if (ghost) return;
        Vec3 panel = c.subtract(n.scale(0.004));
        quadColor(pose, out, panel, r, u, hw, hh, PANEL_COLOR, light);
        String line = video.statusLine(s);
        if (line == null) return;
        text(pose, out, mc.font, c.add(n.scale(0.002)), r, u, n, s, line);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 r, Vec3 u, float dx, float dy, float tu, float tv, int light) {
        float x = (float) (c.x + r.x * dx + u.x * dy);
        float y = (float) (c.y + r.y * dx + u.y * dy);
        float z = (float) (c.z + r.z * dx + u.z * dy);
        vc.addVertex(m, x, y, z).setColor(0xFFFFFFFF).setUv(tu, tv).setLight(light);
    }

    private static void quadColor(PoseStack pose, SubmitNodeCollector out, Vec3 c, Vec3 r, Vec3 u, float hw, float hh, int color, int light) {
        out.submitCustomGeometry(pose, RenderTypes.textBackground(), (p, vc) -> {
            Matrix4f m = p.pose();
            float[][] k = {{-hw, hh}, {-hw, -hh}, {hw, -hh}, {hw, hh}};
            for (int i = 0; i < 4; i++) colorVertex(vc, m, c, r, u, k[i][0], k[i][1], color, light);
            for (int i = 3; i >= 0; i--) colorVertex(vc, m, c, r, u, k[i][0], k[i][1], color, light);
        });
    }

    private static void colorVertex(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 r, Vec3 u, float dx, float dy, int color, int light) {
        float x = (float) (c.x + r.x * dx + u.x * dy);
        float y = (float) (c.y + r.y * dx + u.y * dy);
        float z = (float) (c.z + r.z * dx + u.z * dy);
        vc.addVertex(m, x, y, z).setColor(color).setLight(light);
    }

    /** Centered status text in the plane of the screen (X = right, Y = down). */
    private static void text(PoseStack pose, SubmitNodeCollector out, Font font, Vec3 at, Vec3 r, Vec3 u, Vec3 n, MediaScreen s, String line) {
        pose.pushPose();
        pose.translate(at.x, at.y, at.z);
        Vec3 d = u.scale(-1);
        Vec3 z = r.cross(d);
        Matrix4f basis = new Matrix4f(
                (float) r.x, (float) r.y, (float) r.z, 0f,
                (float) d.x, (float) d.y, (float) d.z, 0f,
                (float) z.x, (float) z.y, (float) z.z, 0f,
                0f, 0f, 0f, 1f);
        pose.mulPose(basis);
        float maxW = s.width * 0.9f;
        int pw = Math.max(1, font.width(line));
        float scale = Math.min(s.height / 9f / 7f, maxW / pw);
        pose.scale(scale, scale, scale);
        Component c = Component.literal(line);
        out.submitText(pose, -pw / 2f, -4f, c.getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET, FULL_BRIGHT, 0xFFD8D8E0, 0, 0);
        pose.popPose();
    }
}
