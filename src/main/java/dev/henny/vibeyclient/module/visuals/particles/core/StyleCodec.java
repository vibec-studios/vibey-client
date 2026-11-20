package dev.henny.vibeyclient.module.visuals.particles.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StyleCodec {
    static final String AMOUNT = "amount", SIZE = "size", TINT = "tint", STRENGTH = "strength", HIDDEN = "hidden";

    private StyleCodec() {}

    public static JsonObject write(ParticleStyles styles) {
        JsonObject out = new JsonObject();
        styles.all().forEach((id, s) -> out.add(id, write(s)));
        return out;
    }

    public static JsonObject write(ParticleStyle s) {
        JsonObject o = new JsonObject();
        if (s.amount() != 100) o.addProperty(AMOUNT, s.amount());
        if (s.size() != 100) o.addProperty(SIZE, s.size());
        if (s.tinted() || s.tint() != ParticleStyle.WHITE) o.addProperty(TINT, TintMath.hex(s.tint()));
        if (s.strength() != 0) o.addProperty(STRENGTH, s.strength());
        if (s.hidden()) o.addProperty(HIDDEN, true);
        return o;
    }

    public static Map<String, ParticleStyle> read(JsonElement json) {
        Map<String, ParticleStyle> out = new LinkedHashMap<>();
        if (json == null || !json.isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
            ParticleStyle s = readOne(e.getValue());
            if (s != null && !s.vanilla()) out.put(e.getKey(), s);
        }
        return out;
    }

    static ParticleStyle readOne(JsonElement json) {
        if (json == null || !json.isJsonObject()) return null;
        JsonObject o = json.getAsJsonObject();
        try {
            return new ParticleStyle(integer(o, AMOUNT, 100), integer(o, SIZE, 100),
                    TintMath.parseHex(string(o, TINT), ParticleStyle.WHITE), integer(o, STRENGTH, 0),
                    o.has(HIDDEN) && o.get(HIDDEN).getAsBoolean());
        } catch (RuntimeException bad) {
            return null;
        }
    }

    private static int integer(JsonObject o, String key, int def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : def;
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
}
