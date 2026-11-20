package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.text.Lang;
import dev.henny.companio.text.TextRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TextReader {
    private TextReader() {}

    static TextRef read(JsonObject o, String key, String path, Lang lang, Problems p, TextRef def) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        TextRef t = read(e, Json.at(path, key), lang, p);
        return t == null ? def : t;
    }

    static TextRef read(JsonElement e, String path, Lang lang, Problems p) {
        if (Json.isString(e)) return string(e.getAsString(), path, lang, p);
        if (e.isJsonObject()) return languages(e.getAsJsonObject(), path, p);
        if (e.isJsonArray()) return list(e.getAsJsonArray(), path, lang, p);
        p.error(path, "must be a text, {\"en\": …, \"de\": …} or \"@key\"");
        return null;
    }

    private static TextRef string(String s, String path, Lang lang, Problems p) {
        if (!s.startsWith("@")) return TextRef.literal(s);
        String key = s.substring(1);
        if (!lang.has(key)) p.warn(path, "text key \"" + key + "\" is in no lang/*.json file");
        return TextRef.key(key);
    }

    private static TextRef languages(JsonObject o, String path, Problems p) {
        Map<String, String> m = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (!Json.isString(e.getValue())) {
                p.error(Json.at(path, e.getKey()), "must be a text");
                continue;
            }
            m.put(Lang.base(e.getKey()), e.getValue().getAsString());
        }
        if (!m.containsKey("en")) p.warn(path, "no English (\"en\") text");
        return TextRef.languages(m);
    }

    private static TextRef list(JsonArray a, String path, Lang lang, Problems p) {
        List<TextRef> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            TextRef t = read(a.get(i), Json.at(path, i), lang, p);
            if (t != null) out.add(t);
        }
        if (out.isEmpty()) p.error(path, "the list is empty");
        return TextRef.anyOf(out);
    }
}
