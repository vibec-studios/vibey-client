package dev.henny.vibeyclient.module.visuals.items.core;

public final class GlintTint {
    public static final float[] TEXTURE_ENERGY = {0.1284f, 0.0299f, 0.3348f};
    public static final float MAX_GAIN = 6f;

    private GlintTint() {}

    public static float[] modulator(boolean tint, int rgb, float strength) {
        float s = Math.max(0, strength);
        if (!tint) return new float[] {s, s, s, 1};
        float peak = Math.max(TEXTURE_ENERGY[0], Math.max(TEXTURE_ENERGY[1], TEXTURE_ENERGY[2]));
        float[] out = new float[4];
        for (int c = 0; c < 3; c++) {
            float want = ((rgb >> (16 - 8 * c)) & 0xFF) / 255f;
            float gain = (float) Math.sqrt(want * peak / TEXTURE_ENERGY[c]);
            out[c] = Math.min(MAX_GAIN, gain) * s;
        }
        out[3] = 1;
        return out;
    }
}
