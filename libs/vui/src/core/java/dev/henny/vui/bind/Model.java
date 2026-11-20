package dev.henny.vui.bind;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Model {
    private final Map<String, Object> entries = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public Model value(String name, Object value) {
        Object existing = entries.get(name);
        if (existing instanceof State<?> s) ((State<Object>) s).set(value);
        else entries.put(name, new State<>(value));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> State<T> state(String name) {
        Object existing = entries.get(name);
        if (existing instanceof State<?> s) return (State<T>) s;
        State<T> created = new State<>(null);
        entries.put(name, created);
        return created;
    }

    public Model state(String name, State<?> state) {
        entries.put(name, state);
        return this;
    }

    public <T> ListState<T> list(String name, Collection<? extends T> initial) {
        ListState<T> list = new ListState<>(initial);
        entries.put(name, list);
        return list;
    }

    public Model list(String name, ListState<?> list) {
        entries.put(name, list);
        return this;
    }

    public Model computed(String name, Supplier<?> formula) {
        entries.put(name, new Computed<>(formula));
        return this;
    }

    public Model action(String name, Action action) {
        entries.put(name, action);
        return this;
    }

    public Model action(String name, Runnable action) {
        return action(name, args -> {
            action.run();
            return null;
        });
    }

    public Model action1(String name, Consumer<Object> action) {
        return action(name, args -> {
            action.accept(args.length > 0 ? args[0] : null);
            return null;
        });
    }

    public boolean has(String name) {
        return entries.containsKey(name);
    }

    public Object entry(String name) {
        return entries.get(name);
    }

    public Object get(String name) {
        Object entry = entries.get(name);
        if (entry instanceof ReadableValue<?> v) return v.get();
        return entry;
    }

    public Set<String> names() {
        return entries.keySet();
    }
}
