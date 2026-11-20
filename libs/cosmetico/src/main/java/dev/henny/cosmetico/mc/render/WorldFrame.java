package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;

public final class WorldFrame {
    private WorldFrame() {}

    public static void toModelSpace(PoseStack ps) {
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.5f, 0f);
    }

    static double lerp(float t, double a, double b) {
        return a + (b - a) * t;
    }

    static float lerpDeg(float t, float a, float b) {
        float d = ((b - a) % 360f + 540f) % 360f - 180f;
        return a + d * t;
    }
}
