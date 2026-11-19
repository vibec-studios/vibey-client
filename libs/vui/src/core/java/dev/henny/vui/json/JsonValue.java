package dev.henny.vui.json;

import dev.henny.vui.diag.SourcePos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonValue {
    private final Object value;
    private final SourcePos pos;

    public JsonValue(Object value, SourcePos pos) {
        this.value = value;
        this.pos = pos;
    }

    public Object raw() {
        return value;
    }

    public SourcePos pos() {
        return pos;
    }

    public boolean isObject() {
        return value instanceof Map;
    }

    public boolean isArray() {
        return value instanceof List;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isNull() {
        return value == null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, JsonValue> asObject() {
        return isObject() ? (Map<String, JsonValue>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    public List<JsonValue> asArray() {
        return isArray() ? (List<JsonValue>) value : List.of();
    }

    public String asString() {
        return value == null ? null : String.valueOf(toPlain());
    }

    public JsonValue get(String key) {
        return asObject().get(key);
    }

    public Object toPlain() {
        if (value instanceof Map<?, ?>) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, JsonValue> e : asObject().entrySet()) out.put(e.getKey(), e.getValue().toPlain());
            return out;
        }
        if (value instanceof List<?>) {
            List<Object> out = new ArrayList<>();
            for (JsonValue v : asArray()) out.add(v.toPlain());
            return out;
        }
        return value;
    }
}
