package dev.henny.vibeyclient.module.waypoints.core;

public final class Dimensions {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = "minecraft:the_end";

    private Dimensions() {}

    public static String shortCode(String dimension) {
        if (OVERWORLD.equals(dimension)) return "o";
        if (NETHER.equals(dimension)) return "n";
        if (END.equals(dimension)) return "e";
        return dimension;
    }

    public static String fromShortCode(String code) {
        return switch (code) {
            case "o" -> OVERWORLD;
            case "n" -> NETHER;
            case "e" -> END;
            default -> code;
        };
    }

    public static boolean valid(String dimension) {
        return dimension != null && dimension.matches("[a-z0-9_.-]{1,64}:[a-z0-9_./-]{1,128}");
    }
}
