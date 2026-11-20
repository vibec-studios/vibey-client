package dev.henny.cosmetico.pose;

import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.mesh.ModelSpace;
import dev.henny.cosmetico.model.PlayerBone;

public final class EmotePartPose {
    private EmotePartPose() {}

    public static boolean active(EmotePlay e) {
        return e != null && e.weight > 0f;
    }

    public static void blend(EmotePlay e, PlayerBone b, PartPose part) {
        int ch = e.channels[b.ordinal()];
        if (ch == 0) return;
        float w = e.weight;
        float[] p = e.pose;
        int o = b.ordinal() * 9;
        final float d = ModelSpace.DEG;
        if ((ch & 1) != 0) {
            part.xRot = lerp(part.xRot, -p[o] * d, w);
            part.yRot = lerp(part.yRot, -p[o + 1] * d, w);
            part.zRot = lerp(part.zRot, p[o + 2] * d, w);
        }
        if ((ch & 2) != 0) {
            part.x = lerp(part.x, -b.px - p[o + 3], w);
            part.y = lerp(part.y, 24f - b.py - p[o + 4], w);
            part.z = lerp(part.z, b.pz + p[o + 5], w);
        }
        if ((ch & 4) != 0) {
            part.xScale = lerp(part.xScale, p[o + 6], w);
            part.yScale = lerp(part.yScale, p[o + 7], w);
            part.zScale = lerp(part.zScale, p[o + 8], w);
        }
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
