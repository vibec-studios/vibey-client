package dev.henny.r3.fx;

import dev.henny.r3.math.Argb;

public final class ColorRamp {
    private final float[] positions;
    private final int[] colors;

    private ColorRamp(float[] positions, int[] colors) {
        this.positions = positions;
        this.colors = colors;
    }

    public static ColorRamp constant(int argb) {
        return new ColorRamp(new float[] {0f}, new int[] {argb});
    }

    public static ColorRamp fade(int from, int to) {
        return new ColorRamp(new float[] {0f, 1f}, new int[] {from, to});
    }

    public static ColorRamp fadeOut(int argb) {
        return fade(argb, argb & 0x00FFFFFF);
    }

    public static ColorRamp of(float[] positions, int[] colors) {
        if (positions.length == 0 || positions.length != colors.length) throw new IllegalArgumentException("bad ramp");
        for (int i = 1; i < positions.length; i++) {
            if (positions[i] < positions[i - 1]) throw new IllegalArgumentException("ramp positions must grow");
        }
        return new ColorRamp(positions.clone(), colors.clone());
    }

    public int at(float t) {
        if (t <= positions[0]) return colors[0];
        int last = positions.length - 1;
        if (t >= positions[last]) return colors[last];
        int i = 1;
        while (positions[i] < t) i++;
        float span = positions[i] - positions[i - 1];
        float k = span <= 0f ? 1f : (t - positions[i - 1]) / span;
        return Argb.lerp(colors[i - 1], colors[i], k);
    }
}
