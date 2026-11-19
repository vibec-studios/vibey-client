package dev.henny.vui.markup;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.json.JsonValue;

import java.util.Map;

public final class PropsBlock {
    private PropsBlock() {
    }

    public static void readProps(JsonValue json, Map<String, Object> props, Diagnostics diagnostics) {
        if (json.isArray()) {
            for (JsonValue name : json.asArray()) {
                if (name.isString()) props.put(name.asString(), null);
                else diagnostics.error(name.pos(), "prop names must be strings", null);
            }
            return;
        }
        if (!json.isObject()) {
            diagnostics.error(json.pos(), "props must be an object of defaults or an array of names",
                    "e.g. { \"title\": \"Untitled\", \"count\": 0 }");
            return;
        }
        for (Map.Entry<String, JsonValue> e : json.asObject().entrySet()) props.put(e.getKey(), e.getValue().toPlain());
    }

    public static void readScript(JsonValue json, Map<String, Object> props, Map<String, String> components,
                                  Diagnostics diagnostics) {
        if (!json.isObject()) {
            diagnostics.error(json.pos(), "script JSON must be an object", "e.g. { \"props\": {…}, \"components\": {…} }");
            return;
        }
        for (Map.Entry<String, JsonValue> e : json.asObject().entrySet()) {
            switch (e.getKey()) {
                case "props" -> readProps(e.getValue(), props, diagnostics);
                case "components" -> readComponents(e.getValue(), components, diagnostics);
                default -> diagnostics.warn(e.getValue().pos(), "unknown script key '" + e.getKey() + "'", "known: props, components");
            }
        }
    }

    public static void readComponents(JsonValue json, Map<String, String> components, Diagnostics diagnostics) {
        if (!json.isObject()) {
            diagnostics.error(json.pos(), "components must map tag names to document ids", "e.g. { \"card\": \"mymod:card\" }");
            return;
        }
        for (Map.Entry<String, JsonValue> e : json.asObject().entrySet()) {
            if (e.getValue().isString()) components.put(e.getKey(), e.getValue().asString());
            else diagnostics.error(e.getValue().pos(), "component id must be a string", null);
        }
    }
}
