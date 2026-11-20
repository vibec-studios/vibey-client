package dev.henny.vui.contract;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.json.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ContractReader {
    public static final Set<String> TYPES = Set.of("any", "string", "number", "bool", "list", "object");

    private ContractReader() {
    }

    public static Contract read(JsonValue json, Diagnostics diagnostics) {
        if (!json.isObject()) {
            diagnostics.error(json.pos(), "contract must be an object", "e.g. { \"fields\": { \"title\": \"string\" }, \"actions\": [\"close\"] }");
            return Contract.EMPTY;
        }
        List<Contract.Entry> fields = new ArrayList<>();
        List<Contract.Entry> actions = new ArrayList<>();
        for (Map.Entry<String, JsonValue> e : json.asObject().entrySet()) {
            switch (e.getKey()) {
                case "fields" -> entries(e.getValue(), fields, true, diagnostics);
                case "actions" -> entries(e.getValue(), actions, false, diagnostics);
                default -> diagnostics.warn(e.getValue().pos(), "unknown contract key '" + e.getKey() + "'", "known: fields, actions");
            }
        }
        return new Contract(List.copyOf(fields), List.copyOf(actions));
    }

    private static void entries(JsonValue json, List<Contract.Entry> out, boolean typed, Diagnostics diagnostics) {
        if (json.isArray()) {
            for (JsonValue v : json.asArray()) {
                if (v.isString()) add(v.asString(), "any", v, out, diagnostics);
                else diagnostics.error(v.pos(), "contract names must be strings", null);
            }
            return;
        }
        if (!json.isObject()) {
            diagnostics.error(json.pos(), "expected a list of names or an object name → type", null);
            return;
        }
        for (Map.Entry<String, JsonValue> e : json.asObject().entrySet()) {
            String type = typed && e.getValue().isString() ? e.getValue().asString().trim() : "any";
            add(e.getKey(), type, e.getValue(), out, diagnostics);
        }
    }

    private static void add(String raw, String type, JsonValue at, List<Contract.Entry> out, Diagnostics diagnostics) {
        String name = raw.trim();
        int paren = name.indexOf('(');
        if (paren >= 0) name = name.substring(0, paren).trim();
        boolean optional = name.endsWith("?");
        if (optional) name = name.substring(0, name.length() - 1);
        String t = type.equals("boolean") ? "bool" : type.equals("array") ? "list" : type.equals("map") ? "object" : type;
        if (!TYPES.contains(t)) {
            diagnostics.warn(at.pos(), "unknown contract type '" + type + "' for '" + name + "'", "use one of " + TYPES);
            t = "any";
        }
        out.add(new Contract.Entry(name, t, optional, at.pos()));
    }
}
