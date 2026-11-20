package dev.henny.vibeyclient.module.waypoints.core;

public final class Colors {
    public static final int[] PALETTE = {0x8B6CFF, 0x3FE0C5, 0xFFE14D, 0xFF8A3B, 0xFF4D6A, 0x4D9BFF, 0x5BEA6B, 0xFFFFFF};
    public static final int DEATH = 0xFF3B3B;

    private Colors() {}

    public static int next(int count) {
        return PALETTE[Math.floorMod(count, PALETTE.length)];
    }

    public static String hex(int rgb) {
        return String.format("%06x", rgb & 0xFFFFFF);
    }

    public static int parseHex(String hex) {
        if (hex == null || !hex.matches("#?[0-9a-fA-F]{6}")) throw new IllegalArgumentException("colour");
        return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16);
    }
}
