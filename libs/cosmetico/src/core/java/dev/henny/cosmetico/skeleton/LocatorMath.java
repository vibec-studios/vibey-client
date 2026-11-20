package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.model.Locator;
import dev.henny.cosmetico.model.PlayerBone;
import org.joml.Vector3f;

public final class LocatorMath {
    private LocatorMath() {}

    private static final float[][] PART_PIVOT_M = new float[PlayerBone.VALUES.length][];

    static {
        for (PlayerBone pb : PlayerBone.VALUES)
            PART_PIVOT_M[pb.ordinal()] = pb == PlayerBone.ROOT ? new float[]{0f, 0f, 0f} : new float[]{-pb.px, 24f - pb.py, pb.pz};
    }

    public static float[] partPivot(PlayerBone part) {
        return PART_PIVOT_M[part.ordinal()];
    }

    public static Vector3f local(Locator l, float[] bonePivotM, Vector3f out) {
        float[] p = l.position();
        return out.set((-p[0] - bonePivotM[0]) / 16f, (24f - p[1] - bonePivotM[1]) / 16f, (p[2] - bonePivotM[2]) / 16f);
    }
}
