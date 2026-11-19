package dev.henny.vibeyclient.ranks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public record Label(String en, String de) {
    public static Label of(JsonElement e, String fallback) {
        if (e instanceof JsonPrimitive p && p.isString()) return new Label(p.getAsString(), null);
        if (!(e instanceof JsonObject o)) return new Label(fallback, null);
        String en = Json.text(o, "en", null);
        return new Label(en == null ? fallback : en, Json.text(o, "de", null));
    }

    public String get(boolean german) {
        return german && de != null && !de.isBlank() ? de : en;
    }
}
