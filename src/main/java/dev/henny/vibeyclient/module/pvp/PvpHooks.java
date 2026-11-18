package dev.henny.vibeyclient.module.pvp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** What the pvp mixins ask, in one place (each answer already includes "module on" and the server rules). */
public final class PvpHooks {
    private PvpHooks() {}

    /** Vanilla's quad is 1 unit tall at 0.5 in front of the eye; 100 % moves it this far down. */
    private static final float FIRE_MAX = 0.32f, SHIELD_MAX = 0.25f;

    // ---------------------------------------------------------------------------------------- crosshair

    /** Draws the custom crosshair instead of vanilla's sprite; false = draw vanilla. */
    public static boolean crosshair(GuiGraphics g) {
        return Crosshair.drawHud(g);
    }

    // ---------------------------------------------------------------------------------------- block outline

    public static boolean outlineActive() {
        PvpModule m = PvpModule.get();
        return m != null && m.outlineActive();
    }

    public static boolean outlineFill() {
        PvpModule m = PvpModule.get();
        return m != null && m.outlineFill.on();
    }

    public static int outlineColor() {
        PvpModule m = PvpModule.get();
        if (m == null) return 0x66000000;
        return (m.outlineColor.argb() & 0xFFFFFF) | (Math.round(m.outlineOpacity.get() * 2.55f) << 24);
    }

    public static float outlineWidth() {
        PvpModule m = PvpModule.get();
        return m == null ? 2.5f : m.outlineWidth.get();
    }

    /** Translucent faces of every box of the outline shape, a hair bigger than the block (no z-fighting). */
    public static void fillOutline(PoseStack poseStack, VertexConsumer vc, BlockOutlineRenderState state, Vec3 cam) {
        PvpModule m = PvpModule.get();
        if (m == null) return;
        int color = (m.outlineColor.argb() & 0xFFFFFF) | (Math.round(m.outlineFillOpacity.get() * 2.55f) << 24);
        BlockPos p = state.pos();
        double ox = p.getX() - cam.x, oy = p.getY() - cam.y, oz = p.getZ() - cam.z;
        Matrix4f pose = poseStack.last().pose();
        float e = 0.002f;
        state.shape().forAllBoxes((x1, y1, z1, x2, y2, z2) -> box(vc, pose,
                (float) (x1 + ox) - e, (float) (y1 + oy) - e, (float) (z1 + oz) - e,
                (float) (x2 + ox) + e, (float) (y2 + oy) + e, (float) (z2 + oz) + e, color));
    }

    /** Six outward-facing quads (counter-clockwise seen from outside, the debug box type culls back faces). */
    private static void box(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        // bottom (y1), seen from below
        quad(vc, m, c, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        // top (y2)
        quad(vc, m, c, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        // north (z1)
        quad(vc, m, c, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        // south (z2)
        quad(vc, m, c, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        // west (x1)
        quad(vc, m, c, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        // east (x2)
        quad(vc, m, c, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int c, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vc.addVertex(m, ax, ay, az).setColor(c);
        vc.addVertex(m, bx, by, bz).setColor(c);
        vc.addVertex(m, cx, cy, cz).setColor(c);
        vc.addVertex(m, dx, dy, dz).setColor(c);
    }

    // ---------------------------------------------------------------------------------------- first person

    /** How far to move the fire overlay down (0 = vanilla). */
    public static float fireOffset() {
        PvpModule m = PvpModule.get();
        return m != null && m.lowFireActive() ? m.fireHeight.get() / 100f * FIRE_MAX : 0f;
    }

    /** How far to move a held shield down (0 = vanilla). */
    public static float shieldOffset() {
        PvpModule m = PvpModule.get();
        return m != null && m.lowShieldActive() ? m.shieldHeight.get() / 100f * SHIELD_MAX : 0f;
    }
}
