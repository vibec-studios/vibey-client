package dev.henny.vibeyclient.module.info.core;

public final class Colors {
    private Colors() {}

    public static int lerp(int from, int to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int a = mix(from >>> 24, to >>> 24, k);
        int r = mix((from >> 16) & 0xFF, (to >> 16) & 0xFF, k);
        int g = mix((from >> 8) & 0xFF, (to >> 8) & 0xFF, k);
        int b = mix(from & 0xFF, to & 0xFF, k);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int alpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return a << 24 | (argb & 0xFFFFFF);
    }

    private static int mix(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
