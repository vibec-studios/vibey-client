package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

final class JsonRead {
    private JsonRead() {}

    static String str(JsonObject o, String k, String def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    static boolean bool(JsonObject o, String k, boolean def) {
        Boolean b = boolOrNull(o, k);
        return b == null ? def : b;
    }

    static Boolean boolOrNull(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : null;
    }

    static int num(JsonObject o, String k, int def) {
        double d = numF(o, k, Double.NaN);
        return Double.isNaN(d) ? def : (int) Math.round(d);
    }

    static double numF(JsonObject o, String k, double def) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        double d = e.getAsDouble();
        return Double.isFinite(d) ? d : def;
    }

    static Float floatOrNull(JsonObject o, String k) {
        double d = numF(o, k, Double.NaN);
        return Double.isNaN(d) ? null : (float) d;
    }

    static <E extends Enum<E>> E enumOrNull(Class<E> type, JsonObject o, String k) {
        String s = str(o, k, null);
        if (s == null) return null;
        try {
            return Enum.valueOf(type, s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
