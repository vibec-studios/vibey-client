package dev.henny.vibeyclient.module.waypoints.core;

public final class NetherCoords {
    public static final int RATIO = 8;

    private NetherCoords() {}

    public static int toNether(int overworld) {
        return Math.floorDiv(overworld, RATIO);
    }

    public static int toOverworld(int nether) {
        return nether * RATIO;
    }

    public static String counterpart(String dimension) {
        if (Dimensions.OVERWORLD.equals(dimension)) return Dimensions.NETHER;
        if (Dimensions.NETHER.equals(dimension)) return Dimensions.OVERWORLD;
        return null;
    }

    public static Pos convert(Pos p, String from, String to) {
        if (from == null || from.equals(to)) return p;
        if (Dimensions.OVERWORLD.equals(from) && Dimensions.NETHER.equals(to)) {
            return Pos.clamped(toNether(p.x()), p.y(), toNether(p.z()));
        }
        if (Dimensions.NETHER.equals(from) && Dimensions.OVERWORLD.equals(to)) {
            return Pos.clamped((long) p.x() * RATIO, p.y(), (long) p.z() * RATIO);
        }
        return null;
    }
}
