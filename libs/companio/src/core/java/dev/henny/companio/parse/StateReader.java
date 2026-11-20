package dev.henny.companio.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.def.NeedDef;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.Problems;

import java.util.LinkedHashMap;
import java.util.Map;

final class StateReader {
    private StateReader() {}

    static Map<String, NeedDef> needs(JsonObject root, Problems p) {
        Map<String, NeedDef> out = new LinkedHashMap<>();
        JsonObject o = Json.obj(root, "needs", "", p);
        if (o == null) return out;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String at = "needs." + e.getKey();
            if (!e.getKey().matches(PackLimits.NEED_ID)) {
                p.error(at, "need name must be lowercase a-z and _ (at most 24)");
                continue;
            }
            if (out.size() == PackLimits.MAX_NEEDS) {
                p.error(at, "too many needs (at most " + PackLimits.MAX_NEEDS + ")");
                break;
            }
            if (!e.getValue().isJsonObject()) {
                p.error(at, "must be { \"start\": 0.5, \"perMinute\": -0.02 }");
                continue;
            }
            JsonObject n = e.getValue().getAsJsonObject();
            Json.known(n, at, p, "start", "perMinute");
            out.put(e.getKey(), new NeedDef(e.getKey(), Json.num(n, "start", at, p, 0.5f, 0f, 1f), Json.num(n, "perMinute", at, p, 0f, -10f, 10f)));
        }
        return out;
    }

    static Map<String, Float> vars(JsonObject root, Problems p) {
        Map<String, Float> out = new LinkedHashMap<>();
        JsonObject o = Json.obj(root, "vars", "", p);
        if (o == null) return out;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String at = "vars." + e.getKey();
            if (!e.getKey().matches(PackLimits.NEED_ID)) {
                p.error(at, "var name must be lowercase a-z and _ (at most 24)");
                continue;
            }
            if (out.size() == PackLimits.MAX_VARS) {
                p.error(at, "too many vars (at most " + PackLimits.MAX_VARS + ")");
                break;
            }
            if (!Json.isNumber(e.getValue())) {
                p.error(at, "must be a number (the start value)");
                continue;
            }
            out.put(e.getKey(), e.getValue().getAsFloat());
        }
        return out;
    }
}
