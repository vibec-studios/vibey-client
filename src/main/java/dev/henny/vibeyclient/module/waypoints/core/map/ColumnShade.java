package dev.henny.vibeyclient.module.waypoints.core.map;

public final class ColumnShade {
    public static final float LOWER = 0.71f;
    public static final float LEVEL = 0.86f;
    public static final float HIGHER = 1.0f;
    static final float[] WATER = {1.0f, 0.94f, 0.88f, 0.82f, 0.76f, 0.70f, 0.66f, 0.62f};

    private ColumnShade() {}

    public static int shade(int packed, short height, short north) {
        if (height == TileData.UNKNOWN) return 0;
        int water = TileData.water(packed);
        float f;
        if (water > 0) f = WATER[Math.min(WATER.length - 1, (water - 1) / 2)];
        else if (north == TileData.UNKNOWN || north == height) f = LEVEL;
        else f = height > north ? HIGHER : LOWER;
        return 0xFF000000 | scale(TileData.rgb(packed), f);
    }

    static int scale(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }
}
