package dev.henny.r3.hint;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SetHintStore implements HintStore {
    private static final Gson GSON = new Gson();
    private final Set<String> seen = new LinkedHashSet<>();
    private Runnable onChange = () -> {};

    @Override
    public boolean seen(String hintId) {
        return seen.contains(hintId);
    }

    @Override
    public void markSeen(String hintId) {
        if (seen.add(hintId)) onChange.run();
    }

    public SetHintStore onChange(Runnable listener) {
        onChange = listener == null ? () -> {} : listener;
        return this;
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(seen);
    }

    public void reset() {
        seen.clear();
        onChange.run();
    }

    public String toJson() {
        JsonArray arr = new JsonArray();
        seen.forEach(arr::add);
        return GSON.toJson(arr);
    }

    public static SetHintStore fromJson(String json) {
        SetHintStore store = new SetHintStore();
        if (json == null || json.isBlank()) return store;
        try {
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root != null && root.isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray()) if (e.isJsonPrimitive()) store.seen.add(e.getAsString());
            }
        } catch (JsonParseException e) {
            return store;
        }
        return store;
    }
}
