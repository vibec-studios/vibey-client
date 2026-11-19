package dev.henny.cosmetico.pose;

import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.model.PlayerBone;
import org.joml.Quaternionf;

public final class RootPose {
    public boolean translate, rotate, scale;
    public float tx, ty, tz, sx = 1f, sy = 1f, sz = 1f;
    public final Quaternionf rotation = new Quaternionf();

    public boolean compute(EmotePlay e, float modelScale) {
        translate = rotate = scale = false;
        if (!EmotePartPose.active(e)) return false;
        int ch = e.channels[PlayerBone.ROOT.ordinal()];
        if (ch == 0) return false;
        float w = e.weight;
        float[] p = e.pose;
        int o = PlayerBone.ROOT.ordinal() * 9;
        if ((ch & 2) != 0) {
            float k = w * modelScale / 16f;
            translate = true;
            tx = p[o + 3] * k;
            ty = p[o + 4] * k;
            tz = p[o + 5] * k;
        }
        if ((ch & 1) != 0) {
            final float d = (float) (Math.PI / 180.0) * w;
            rotate = true;
            rotation.rotationZYX(p[o + 2] * d, p[o + 1] * d, p[o] * d);
        }
        if ((ch & 4) != 0) {
            scale = true;
            sx = EmotePartPose.lerp(1f, p[o + 6], w);
            sy = EmotePartPose.lerp(1f, p[o + 7], w);
            sz = EmotePartPose.lerp(1f, p[o + 8], w);
        }
        return true;
    }
}
