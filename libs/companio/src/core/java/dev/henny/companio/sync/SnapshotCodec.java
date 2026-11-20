package dev.henny.companio.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.text.Sanitize;

import java.util.ArrayList;
import java.util.List;

public final class SnapshotCodec {
    public static final int VERSION = 1;
    public static final int MAX_BYTES = 1024;
    private static final String NAME = "[a-z0-9_]{1,64}";
    private static final String HASH = "[0-9a-f]{16}";

    public static final String GONE = "{\"v\":1,\"gone\":true}";

    private SnapshotCodec() {}

    public static boolean gone(String text) {
        return GONE.equals(text);
    }

    public static String encode(Snapshot s) {
        JsonObject o = new JsonObject();
        o.addProperty("v", VERSION);
        o.addProperty("p", s.pack());
        o.addProperty("h", s.hash());
        o.addProperty("c", s.companion());
        o.addProperty("va", s.variant());
        o.addProperty("n", s.name());
        o.addProperty("s", round(s.size(), 100));
        o.addProperty("x", round(s.x(), 100));
        o.addProperty("y", round(s.y(), 100));
        o.addProperty("z", round(s.z(), 100));
        o.addProperty("r", round(s.yaw(), 10));
        o.addProperty("d", s.dimension());
        o.addProperty("b", s.base() == null ? "" : s.base());
        o.add("l", list(s.layers()));
        o.add("pr", list(s.props()));
        o.addProperty("t", s.bubble());
        o.addProperty("g", s.held());
        return o.toString();
    }

    public static Snapshot decode(String text) {
        if (text == null || text.length() > MAX_BYTES) return null;
        try {
            JsonElement e = JsonParser.parseString(text);
            if (!e.isJsonObject()) return null;
            JsonObject o = e.getAsJsonObject();
            if (o.get("v").getAsInt() != VERSION) return null;
            String pack = o.get("p").getAsString(), hash = o.get("h").getAsString(), comp = o.get("c").getAsString();
            if (!pack.matches(PackLimits.PACK_ID) || !hash.matches(HASH) || !comp.matches(PackLimits.ID)) return null;
            double x = o.get("x").getAsDouble(), y = o.get("y").getAsDouble(), z = o.get("z").getAsDouble();
            float size = o.get("s").getAsFloat(), yaw = o.get("r").getAsFloat();
            if (!finite(x, y, z, size, yaw) || Math.abs(x) > 3e7 || Math.abs(z) > 3e7 || Math.abs(y) > 4096) return null;
            return new Snapshot(pack, hash, comp, id(o, "va", "default"), Sanitize.name(str(o, "n")), Math.max(0.2f, Math.min(4f, size)),
                    x, y, z, yaw, str(o, "d"), id(o, "b", ""), names(o, "l", 4), names(o, "pr", 16),
                    Sanitize.bubble(str(o, "t")), o.has("g") && o.get("g").getAsBoolean());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean finite(double... v) {
        for (double d : v) if (!Double.isFinite(d)) return false;
        return true;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    private static String id(JsonObject o, String k, String def) {
        String s = str(o, k);
        return s.matches(NAME) ? s : def;
    }

    private static List<String> names(JsonObject o, String k, int max) {
        List<String> out = new ArrayList<>();
        if (!o.has(k) || !o.get(k).isJsonArray()) return out;
        for (JsonElement e : o.getAsJsonArray(k)) {
            if (out.size() >= max) break;
            if (e.isJsonPrimitive() && e.getAsString().matches(NAME)) out.add(e.getAsString());
        }
        return List.copyOf(out);
    }

    private static JsonArray list(List<String> l) {
        JsonArray a = new JsonArray();
        if (l != null) for (String s : l) a.add(s);
        return a;
    }

    private static double round(double v, double f) {
        return Math.round(v * f) / f;
    }
}
