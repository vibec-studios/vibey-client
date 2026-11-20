package dev.henny.vibeyclient.module.visuals.particles.core;

import java.util.Locale;

public final class TintMath {
    public static final float FLOOR = 0.35f;

    private TintMath() {}

    public static int apply(int argb, int tintRgb, int strength) {
        if (strength <= 0) return argb;
        float k = Math.min(100, strength) / 100f;
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        float light = FLOOR + (1 - FLOOR) * Math.max(r, Math.max(g, b)) / 255f;
        int tr = Math.round(((tintRgb >> 16) & 255) * light);
        int tg = Math.round(((tintRgb >> 8) & 255) * light);
        int tb = Math.round((tintRgb & 255) * light);
        return (a << 24) | (mix(r, tr, k) << 16) | (mix(g, tg, k) << 8) | mix(b, tb, k);
    }

    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06x", rgb & 0xFFFFFF);
    }

    public static int parseHex(String text, int fallback) {
        if (text == null) return fallback;
        String t = text.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() == 8) t = t.substring(2);
        if (t.length() != 6) return fallback;
        try {
            return Integer.parseInt(t, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int mix(int from, int to, float k) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * k)));
    }
}
