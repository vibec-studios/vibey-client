package dev.henny.vibeyclient.ranks.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class NameStyleJson {
    private NameStyleJson() {}

    public static NameStyle parse(JsonObject spec) {
        if (spec == null) return null;
        JsonObject p = spec.get("params") instanceof JsonObject o ? o : new JsonObject();
        return switch (text(spec, "effect", "")) {
            case "gradient" -> gradient(p);
            case "wave" -> wave(p);
            case "rainbow" -> new RainbowStyle(num(p, "saturation", 0.75), num(p, "lightness", 0.62), num(p, "speed", 0.35), num(p, "spread", 0.06), rtl(p));
            case "pulse" -> pulse(p);
            case "shimmer" -> shimmer(p);
            default -> null;
        };
    }

    private static NameStyle gradient(JsonObject p) {
        int[] c = colors(p, "colors", 2);
        return c == null ? null : new GradientStyle(c, num(p, "speed", 0), rtl(p));
    }

    private static NameStyle wave(JsonObject p) {
        int[] c = colors(p, "colors", 2);
        return c == null ? null : new WaveStyle(c, num(p, "speed", 0.8), Math.max(1, num(p, "wavelength", 6)), rtl(p));
    }

    private static NameStyle pulse(JsonObject p) {
        int[] c = colors(p, "colors", 1);
        return c == null ? null : new PulseStyle(c, num(p, "speed", 0.6), num(p, "min", 0.55));
    }

    private static NameStyle shimmer(JsonObject p) {
        int base = ColorMath.parseHex(text(p, "base", "#ffffff"));
        int highlight = ColorMath.parseHex(text(p, "highlight", "#ffffff"));
        return new ShimmerStyle(base, highlight, Math.max(1, num(p, "width", 3)), num(p, "speed", 0.4), Math.max(0, num(p, "pause", 1)), rtl(p));
    }

    private static int[] colors(JsonObject p, String key, int min) {
        if (!(p.get(key) instanceof JsonArray a) || a.size() < min) return null;
        int n = Math.min(4, a.size());
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = ColorMath.parseHex(a.get(i) instanceof JsonPrimitive s ? s.getAsString() : null);
        return out;
    }

    private static boolean rtl(JsonObject p) {
        return "rtl".equals(text(p, "direction", "ltr"));
    }

    private static double num(JsonObject p, String key, double def) {
        JsonElement e = p.get(key);
        return e instanceof JsonPrimitive n && n.isNumber() ? n.getAsDouble() : def;
    }

    private static String text(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e instanceof JsonPrimitive s && s.isString() ? s.getAsString() : def;
    }
}
