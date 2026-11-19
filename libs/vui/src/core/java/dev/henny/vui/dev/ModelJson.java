package dev.henny.vui.dev;

import dev.henny.vui.bind.Model;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.json.JsonValue;
import dev.henny.vui.json.PosJsonParser;

import java.util.List;
import java.util.Map;

public final class ModelJson {
    private ModelJson() {
    }

    public static Model read(String fileName, String json, Diagnostics diagnostics) {
        Model model = new Model();
        JsonValue root = PosJsonParser.parse(json, fileName, diagnostics);
        if (root == null) return model;
        if (!root.isObject()) {
            diagnostics.error(root.pos(), "state file must be a JSON object", "e.g. { \"title\": \"Hi\", \"items\": [] }");
            return model;
        }
        for (Map.Entry<String, JsonValue> e : root.asObject().entrySet()) {
            Object plain = e.getValue().toPlain();
            if (plain instanceof List<?> list) model.list(e.getKey(), list);
            else model.value(e.getKey(), plain);
        }
        model.action("noop", args -> null);
        return model;
    }
}
