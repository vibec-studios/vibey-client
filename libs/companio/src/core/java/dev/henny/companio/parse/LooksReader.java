package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.BoxDef;
import dev.henny.companio.def.LocoState;
import dev.henny.companio.def.SizeDef;
import dev.henny.companio.def.VariantDef;
import dev.henny.companio.pack.PackPaths;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.text.Lang;
import dev.henny.companio.text.TextRef;
import dev.henny.cosmetico.model.Bundle;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LooksReader {
    private LooksReader() {}

    static List<VariantDef> variants(JsonObject root, Lang lang, Problems p) {
        List<VariantDef> out = new ArrayList<>();
        JsonArray a = Json.arr(root, "variants", "", p);
        if (a == null) return out;
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < a.size(); i++) {
            String at = Json.at("variants", i);
            if (!a.get(i).isJsonObject()) {
                p.error(at, "must be { \"id\": …, \"name\": … }");
                continue;
            }
            JsonObject v = a.get(i).getAsJsonObject();
            Json.known(v, at, p, "id", "name", "textures");
            String id = Json.str(v, "id", at, p, null);
            if (id == null || !AssetReader.id(id, Json.at(at, "id"), p)) continue;
            if (!ids.add(id)) p.error(Json.at(at, "id"), "variant \"" + id + "\" exists twice");
            out.add(new VariantDef(id, TextReader.read(v, "name", at, lang, p, TextRef.literal(id)), textures(v, at, p)));
        }
        return out;
    }

    private static Map<String, String> textures(JsonObject v, String at, Problems p) {
        Map<String, String> m = new LinkedHashMap<>();
        JsonObject t = Json.obj(v, "textures", at, p);
        if (t == null) return m;
        for (Map.Entry<String, JsonElement> e : t.entrySet()) {
            String path = Json.at(Json.at(at, "textures"), e.getKey());
            if (!Json.isString(e.getValue()) || !PackPaths.valid(e.getValue().getAsString())) p.error(path, "must be a path to a PNG in the pack");
            else m.put(e.getKey(), e.getValue().getAsString());
        }
        return m;
    }

    static SizeDef size(JsonObject root, Problems p) {
        JsonObject o = Json.obj(root, "size", "", p);
        if (o == null) return SizeDef.ONE;
        Json.known(o, "size", p, "default", "min", "max");
        float min = Json.num(o, "min", "size", p, 1f, 0.2f, 4f);
        float max = Json.num(o, "max", "size", p, Math.max(1f, min), 0.2f, 4f);
        if (max < min) p.error("size.max", "must not be smaller than min");
        float def = Json.num(o, "default", "size", p, Math.max(min, Math.min(max, 1f)), 0.2f, 4f);
        return new SizeDef(Math.max(min, Math.min(Math.max(min, max), def)), min, Math.max(min, max));
    }

    static BoxDef box(JsonObject root, Problems p) {
        JsonObject o = Json.obj(root, "box", "", p);
        if (o == null) return BoxDef.DEFAULT;
        Json.known(o, "box", p, "width", "height");
        return new BoxDef(Json.num(o, "width", "box", p, 0.6f, 0.1f, 4f), Json.num(o, "height", "box", p, 0.6f, 0.1f, 4f));
    }

    static Map<LocoState, String> loco(JsonObject root, Bundle model, Problems p) {
        Map<LocoState, String> given = new EnumMap<>(LocoState.class);
        JsonObject o = Json.obj(root, "animations", "", p);
        if (o != null) {
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                String at = "animations." + e.getKey();
                LocoState s = LocoState.of(e.getKey());
                if (s == null) {
                    p.warn(at, "unknown movement \"" + e.getKey() + "\" (idle, walk, run, swim, swim_idle, fly, fall, sit, sleep, held, ride)");
                    continue;
                }
                if (!Json.isString(e.getValue())) {
                    p.error(at, "must be the name of a model animation");
                    continue;
                }
                String anim = e.getValue().getAsString();
                if (model != null && model.animation(anim) == null) p.error(at, "the model has no animation \"" + anim + "\" (" + String.join(", ", model.animations.keySet()) + ")");
                else given.put(s, anim);
            }
        }
        Map<LocoState, String> out = new EnumMap<>(LocoState.class);
        for (LocoState s : LocoState.values()) {
            String a = given.get(s);
            if (a == null && model != null && model.animation(s.id()) != null) a = s.id();
            if (a != null) out.put(s, a);
        }
        for (LocoState s : LocoState.values()) {
            if (out.containsKey(s)) continue;
            LocoState f = s.fallback;
            while (f != null && !out.containsKey(f)) f = f.fallback;
            if (f != null) out.put(s, out.get(f));
        }
        if (model != null && !out.containsKey(LocoState.IDLE)) p.warn("animations.idle", "the model has no \"idle\" animation; it stands still");
        return out;
    }
}
