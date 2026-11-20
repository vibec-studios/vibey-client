package dev.henny.vibeyclient.module.screens.hook;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2f;

public final class PipPose {
    private PipPose() {}

    public static int x(GuiGraphics g, int v) {
        Matrix3x2f m = active(g);
        return m == null ? v : Math.round(m.m00() * v + m.m20());
    }

    public static int y(GuiGraphics g, int v) {
        Matrix3x2f m = active(g);
        return m == null ? v : Math.round(m.m11() * v + m.m21());
    }

    public static float scale(GuiGraphics g, float v) {
        Matrix3x2f m = active(g);
        return m == null ? v : v * m.m00();
    }

    private static Matrix3x2f active(GuiGraphics g) {
        if (!ScreenFrame.transforming(g)) return null;
        Matrix3x2f m = g.pose();
        boolean plain = m.m01() == 0 && m.m10() == 0 && m.m00() > 0 && m.m11() > 0;
        boolean identity = m.m00() == 1 && m.m11() == 1 && m.m20() == 0 && m.m21() == 0;
        return plain && !identity ? m : null;
    }
}
