package dev.henny.companio.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.Problems;
import dev.henny.companio.text.Lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LangReader {
    private static final Pattern FILE = Pattern.compile("lang/([a-zA-Z]{2,3}(_[a-zA-Z0-9]{2,4})?)\\.json");

    private LangReader() {}

    static Lang read(PackFiles files, Problems p) {
        Map<String, Map<String, List<String>>> tables = new HashMap<>();
        for (String path : files.list()) {
            Matcher m = FILE.matcher(path);
            if (!m.matches()) continue;
            p.file(path);
            JsonObject o = JsonFiles.object(files, path, p);
            if (o != null) tables.put(m.group(1).toLowerCase(Locale.ROOT), table(o, p));
        }
        return new Lang(tables);
    }

    private static Map<String, List<String>> table(JsonObject o, Problems p) {
        Map<String, List<String>> t = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            JsonElement v = e.getValue();
            if (Json.isString(v)) {
                t.put(e.getKey(), List.of(v.getAsString()));
            } else if (v.isJsonArray()) {
                List<String> list = new ArrayList<>();
                for (JsonElement x : v.getAsJsonArray()) if (Json.isString(x)) list.add(x.getAsString());
                if (list.isEmpty()) p.error(e.getKey(), "must be a text or a list of texts");
                else t.put(e.getKey(), List.copyOf(list));
            } else {
                p.error(e.getKey(), "must be a text or a list of texts");
            }
        }
        return t;
    }
}
