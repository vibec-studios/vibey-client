package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.model.PlayerBone;
import dev.henny.cosmetico.skeleton.PartFrames;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class Frames {
    private Frames() {}

    public static Matrix4f basePose() {
        return new Matrix4f().translate(0.3f, -1.2f, 2.5f).rotate(new Quaternionf().rotationY(0.7f)).scale(0.9375f);
    }

    public static Matrix3f baseNormal() {
        return new Matrix3f().rotate(new Quaternionf().rotationY(0.7f));
    }

    public static final PartFrames PARTS = Frames::apply;

    static void apply(PlayerBone part, Matrix4f pose, Matrix3f normal) {
        if (part == PlayerBone.ROOT) return;
        int k = part.ordinal() + 1;
        Quaternionf q = new Quaternionf().rotationZYX(0.03f * k, -0.05f * k, 0.1f * k);
        pose.translate(-part.px / 16f, (24f - part.py) / 16f, part.pz / 16f).rotate(q);
        normal.rotate(q);
    }
}
