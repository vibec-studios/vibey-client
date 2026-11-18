package dev.henny.vibeyclient.ui;

/**
 * The "Vibe" design tokens (SPEC §4, from the Vibe Launcher). Square corners, 1 px lines, no gradients.
 * All colors ARGB.
 */
public final class Theme {
    private Theme() {}

    public static final int BG = 0xFF000000;
    public static final int SURFACE = 0xFF0B0B0E;
    public static final int SURFACE2 = 0xFF121217;
    public static final int SURFACE3 = 0xFF18181F;
    public static final int PANEL = 0xFF08080B;

    /** rgba(255,255,255,.09) */
    public static final int LINE = 0x17FFFFFF;
    /** rgba(255,255,255,.16) */
    public static final int LINE2 = 0x29FFFFFF;

    public static final int TEXT = 0xFFF4F4F5;
    public static final int MUTED = 0xFFA1A1AA;
    public static final int DIM = 0xFF6B6B75;

    /** Purple. */
    public static final int BRAND = 0xFF8B6CFF;
    /** Teal. */
    public static final int BRAND2 = 0xFF3FE0C5;
    public static final int OK = BRAND2;
    public static final int DANGER = 0xFFFF5470;
    public static final int WARN = 0xFFFFB547;

    /** Lighter brand for hover. */
    public static final int BRAND_HOVER = 0xFF9E84FF;

    /** Dim layer over the world behind menus. */
    public static final int SCRIM = 0xC8000000;

    /** Standard spacing. */
    public static final int PAD = 10;
    public static final int ROW = 24;

    /** Multiplies the alpha of {@code color} by {@code a} (0..1). */
    public static int alpha(int color, float a) {
        int base = color >>> 24;
        int na = Math.max(0, Math.min(255, Math.round(base * a)));
        return (na << 24) | (color & 0xFFFFFF);
    }

    /** Linear mix of two ARGB colors. */
    public static int mix(int a, int b, float t) {
        if (t <= 0) return a;
        if (t >= 1) return b;
        int r = 0;
        for (int s = 0; s <= 24; s += 8) {
            int ca = (a >>> s) & 0xFF;
            int cb = (b >>> s) & 0xFF;
            r |= (Math.round(ca + (cb - ca) * t) & 0xFF) << s;
        }
        return r;
    }
}
