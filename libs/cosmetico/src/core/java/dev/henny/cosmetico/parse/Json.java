package dev.henny.cosmetico.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Text;

import java.util.Base64;

public final class Json {
    private Json() {}

    public static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    public static double num(JsonObject o, String k, double def) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        double d = e.getAsDouble();
        return Double.isFinite(d) ? d : def;
    }

    public static boolean bool(JsonObject o, String k, boolean def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : def;
    }

    public static Text text(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return new Text(e.getAsString(), null);
        if (!(e instanceof JsonObject o)) return null;
        return new Text(str(o, "en"), str(o, "de"));
    }

    public static float[] vec(JsonElement e, String where, float def) {
        if (e == null || e.isJsonNull()) return new float[]{def, def, def};
        return floats(e, 3, where);
    }

    public static float[] floats(JsonElement e, int n, String where) {
        if (!(e instanceof JsonArray a) || a.size() != n) throw new BundleException(where + " must be an array of " + n + " numbers");
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            JsonElement x = a.get(i);
            if (!x.isJsonPrimitive() || !x.getAsJsonPrimitive().isNumber()) throw new BundleException(where + " must contain numbers");
            out[i] = x.getAsFloat();
            if (!Float.isFinite(out[i])) throw new BundleException(where + " must contain finite numbers");
        }
        return out;
    }

    public static byte[] base64(String s, String where) {
        if (s == null) throw new BundleException(where + ": data is missing");
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw new BundleException(where + ": invalid base64");
        }
    }
}
