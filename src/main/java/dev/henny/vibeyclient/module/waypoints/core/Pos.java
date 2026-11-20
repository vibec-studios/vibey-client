package dev.henny.vibeyclient.module.waypoints.core;

public record Pos(int x, int y, int z) {
    public static final int MAX_XZ = 30_000_000;
    public static final int MIN_Y = -2048;
    public static final int MAX_Y = 4096;

    public static boolean valid(long x, long y, long z) {
        return Math.abs(x) <= MAX_XZ && Math.abs(z) <= MAX_XZ && y >= MIN_Y && y <= MAX_Y;
    }

    public static Pos clamped(long x, long y, long z) {
        return new Pos(clamp(x, -MAX_XZ, MAX_XZ), clamp(y, MIN_Y, MAX_Y), clamp(z, -MAX_XZ, MAX_XZ));
    }

    private static int clamp(long v, int lo, int hi) {
        return (int) Math.max(lo, Math.min(hi, v));
    }
}
