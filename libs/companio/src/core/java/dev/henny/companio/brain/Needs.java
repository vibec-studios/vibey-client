package dev.henny.companio.brain;

import dev.henny.companio.def.NeedDef;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Needs {
    private final Map<String, NeedDef> defs;
    private final Map<String, Float> values = new LinkedHashMap<>();

    public Needs(Map<String, NeedDef> defs) {
        this.defs = defs;
        for (NeedDef d : defs.values()) values.put(d.id(), d.start());
    }

    public float get(String id) {
        Float v = values.get(id);
        return v == null ? 0f : v;
    }

    public void set(String id, float v) {
        if (values.containsKey(id)) values.put(id, Math.max(0f, Math.min(1f, v)));
    }

    public void add(String id, float delta) {
        set(id, get(id) + delta);
    }

    public void tick(float dt, Map<String, Float> perMinute) {
        float minutes = dt / 60f;
        for (NeedDef d : defs.values()) {
            float extra = perMinute == null ? 0f : perMinute.getOrDefault(d.id(), 0f);
            add(d.id(), (d.perMinute() + extra) * minutes);
        }
    }

    public Collection<String> ids() {
        return values.keySet();
    }

    public Map<String, Float> snapshot() {
        return Map.copyOf(values);
    }

    public void restore(Map<String, Float> saved) {
        if (saved == null) return;
        saved.forEach(this::set);
    }
}
