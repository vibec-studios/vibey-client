package dev.henny.vibeyclient.module.chat.filters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.ChatFilters;

import java.util.ArrayList;
import java.util.List;

public final class FilterStore {
    private static final String KEY = "filters";

    private final ChatModule module;
    private final List<String> patterns = new ArrayList<>();
    private volatile ChatFilters compiled = ChatFilters.NONE;
    private int hidden;

    public FilterStore(ChatModule module) {
        this.module = module;
    }

    public void load() {
        patterns.clear();
        JsonElement e = module.settings().data().get(KEY);
        if (e != null && e.isJsonArray()) {
            for (JsonElement p : e.getAsJsonArray()) if (p.isJsonPrimitive()) patterns.add(p.getAsString());
        }
        compiled = ChatFilters.of(patterns);
    }

    public ChatFilters filters() {
        return compiled;
    }

    public List<String> patterns() {
        return List.copyOf(patterns);
    }

    public int hidden() {
        return hidden;
    }

    public void countHidden() {
        hidden++;
    }

    public boolean add(String pattern) {
        String p = pattern == null ? "" : pattern.strip();
        if (!ChatFilters.valid(p) || patterns.contains(p) || patterns.size() >= ChatFilters.MAX_RULES) return false;
        patterns.add(p);
        save();
        return true;
    }

    public void remove(String pattern) {
        if (patterns.remove(pattern)) save();
    }

    private void save() {
        JsonArray a = new JsonArray();
        for (String p : patterns) a.add(p);
        module.settings().data().add(KEY, a);
        module.settings().markDirty();
        compiled = ChatFilters.of(patterns);
    }
}
