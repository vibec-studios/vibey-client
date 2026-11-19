package dev.henny.cosmetico.mesh;

public final class ModelSpace {
    private ModelSpace() {}

    public static final float DEG = (float) (Math.PI / 180.0);

    public static float[] toModel(float[] v) {
        return new float[]{-v[0], 24f - v[1], v[2]};
    }

    public static float[] rotToModel(float[] r) {
        final float d = DEG;
        return new float[]{-r[0] * d, -r[1] * d, r[2] * d};
    }
}
