package dev.henny.vui.style;

import dev.henny.vui.anim.Interpolate;

import java.util.List;
import java.util.Map;

public final class ColorParser {
    private static final Map<String, Integer> NAMED = Map.of(
            "transparent", 0x00000000, "white", 0xFFFFFFFF, "black", 0xFF000000, "red", 0xFFFF0000,
            "green", 0xFF00FF00, "blue", 0xFF0000FF, "yellow", 0xFFFFFF00, "gray", 0xFF808080, "grey", 0xFF808080);

    private ColorParser() {
    }

    public static int parse(String text) {
        String s = text.trim().toLowerCase();
        Integer named = NAMED.get(s);
        if (named != null) return named;
        if (s.startsWith("#")) return hex(s.substring(1));
        if (s.startsWith("0x")) return (int) Long.parseLong(s.substring(2), 16);
        if (s.startsWith("rgb")) return rgb(s);
        if (s.startsWith("alpha(")) return alpha(s);
        if (s.startsWith("mix(")) return mix(s);
        throw new StyleError("'" + text + "' is not a color", "use #rrggbb, #rrggbbaa, rgba(r,g,b,a), alpha($token, .5) or a $token");
    }

    public static boolean looksLikeColor(String text) {
        String s = text.trim().toLowerCase();
        return s.startsWith("#") || s.startsWith("rgb") || s.startsWith("alpha(") || s.startsWith("mix(") || NAMED.containsKey(s);
    }

    private static int hex(String h) {
        try {
            return switch (h.length()) {
                case 3 -> 0xFF000000 | expand(h);
                case 4 -> (Integer.parseInt(h.substring(3, 4).repeat(2), 16) << 24) | expand(h.substring(0, 3));
                case 6 -> 0xFF000000 | Integer.parseInt(h, 16);
                case 8 -> (int) ((Long.parseLong(h.substring(6), 16) << 24) | Long.parseLong(h.substring(0, 6), 16));
                default -> throw new StyleError("'#" + h + "' has " + h.length() + " hex digits", "use 3, 4, 6 or 8 digits (#rrggbbaa)");
            };
        } catch (NumberFormatException e) {
            throw new StyleError("'#" + h + "' is not valid hex", null);
        }
    }

    private static int expand(String rgb) {
        int r = Character.digit(rgb.charAt(0), 16);
        int g = Character.digit(rgb.charAt(1), 16);
        int b = Character.digit(rgb.charAt(2), 16);
        if (r < 0 || g < 0 || b < 0) throw new NumberFormatException();
        return (r * 17 << 16) | (g * 17 << 8) | (b * 17);
    }

    private static int rgb(String s) {
        List<String> args = CssValues.arguments(s);
        if (args.size() < 3) throw new StyleError("rgb() needs 3 or 4 numbers", "e.g. rgba(255, 255, 255, 0.09)");
        int r = channel(args.get(0));
        int g = channel(args.get(1));
        int b = channel(args.get(2));
        float a = args.size() > 3 ? Float.parseFloat(args.get(3)) : 1f;
        return (Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    private static int alpha(String s) {
        List<String> args = CssValues.arguments(s);
        if (args.size() != 2) throw new StyleError("alpha() needs a color and a factor", "e.g. alpha($brand, .5)");
        return Interpolate.withAlpha(parse(args.get(0)), Float.parseFloat(args.get(1)));
    }

    private static int mix(String s) {
        List<String> args = CssValues.arguments(s);
        if (args.size() != 3) throw new StyleError("mix() needs two colors and an amount", "e.g. mix($brand, white, .2)");
        return Interpolate.color(parse(args.get(0)), parse(args.get(1)), Float.parseFloat(args.get(2)));
    }

    private static int channel(String v) {
        return Math.max(0, Math.min(255, Math.round(Float.parseFloat(v.trim()))));
    }

    public static String format(int argb) {
        return String.format("#%06x%02x", argb & 0xFFFFFF, argb >>> 24);
    }
}
