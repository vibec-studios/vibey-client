package dev.henny.r3.fx;

public final class FloatCurve {
    private final float[] positions;
    private final float[] values;

    private FloatCurve(float[] positions, float[] values) {
        this.positions = positions;
        this.values = values;
    }

    public static FloatCurve constant(float value) {
        return new FloatCurve(new float[] {0f}, new float[] {value});
    }

    public static FloatCurve linear(float from, float to) {
        return new FloatCurve(new float[] {0f, 1f}, new float[] {from, to});
    }

    public static FloatCurve popAndShrink(float peak) {
        return new FloatCurve(new float[] {0f, 0.15f, 0.8f, 1f}, new float[] {0f, peak, peak, 0f});
    }

    public static FloatCurve of(float[] positions, float[] values) {
        if (positions.length == 0 || positions.length != values.length) throw new IllegalArgumentException("bad curve");
        return new FloatCurve(positions.clone(), values.clone());
    }

    public float at(float t) {
        if (t <= positions[0]) return values[0];
        int last = positions.length - 1;
        if (t >= positions[last]) return values[last];
        int i = 1;
        while (positions[i] < t) i++;
        float span = positions[i] - positions[i - 1];
        float k = span <= 0f ? 1f : (t - positions[i - 1]) / span;
        return values[i - 1] + (values[i] - values[i - 1]) * k;
    }
}
