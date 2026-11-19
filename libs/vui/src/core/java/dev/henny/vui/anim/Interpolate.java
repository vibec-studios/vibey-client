package dev.henny.vui.anim;

public final class Interpolate {
    private Interpolate() {
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int color(int a, int b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return out;
    }

    public static int withAlpha(int argb, float alpha) {
        int base = argb >>> 24;
        int a = Math.max(0, Math.min(255, Math.round(base * alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(1f, v);
    }
}
