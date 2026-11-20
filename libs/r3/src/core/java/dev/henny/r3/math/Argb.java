package dev.henny.r3.math;

public final class Argb {
    private Argb() {}

    public static int alpha(int argb) {
        return argb >>> 24;
    }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int multiplyAlpha(int argb, float factor) {
        if (factor >= 0.999f) return argb;
        int a = Math.round((argb >>> 24) * Math.max(0f, factor));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int from, int to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int a = mix(from >>> 24, to >>> 24, k);
        int r = mix(from >> 16 & 0xFF, to >> 16 & 0xFF, k);
        int g = mix(from >> 8 & 0xFF, to >> 8 & 0xFF, k);
        int b = mix(from & 0xFF, to & 0xFF, k);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int scaleRgb(int argb, float factor) {
        int r = clamp(Math.round((argb >> 16 & 0xFF) * factor));
        int g = clamp(Math.round((argb >> 8 & 0xFF) * factor));
        int b = clamp(Math.round((argb & 0xFF) * factor));
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    public static int hsv(float hue, float saturation, float value, int alpha) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        int sector = Math.min(5, (int) h);
        float f = h - sector;
        float p = value * (1 - saturation);
        float q = value * (1 - saturation * f);
        float t = value * (1 - saturation * (1 - f));
        return switch (sector) {
            case 0 -> pack(alpha, value, t, p);
            case 1 -> pack(alpha, q, value, p);
            case 2 -> pack(alpha, p, value, t);
            case 3 -> pack(alpha, p, q, value);
            case 4 -> pack(alpha, t, p, value);
            default -> pack(alpha, value, p, q);
        };
    }

    private static int pack(int alpha, float r, float g, float b) {
        return (alpha & 0xFF) << 24 | channel(r) << 16 | channel(g) << 8 | channel(b);
    }

    private static int channel(float c) {
        return clamp(Math.round(c * 255f));
    }

    private static int mix(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static int clamp(int c) {
        return Math.max(0, Math.min(255, c));
    }
}
