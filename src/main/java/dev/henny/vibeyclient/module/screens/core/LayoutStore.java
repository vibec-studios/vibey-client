package dev.henny.vibeyclient.module.screens.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LayoutStore {
    private final Map<String, ScreenLayout> screens = new LinkedHashMap<>();
    private Runnable onChange = () -> {};

    public synchronized ScreenLayout get(String screen) {
        ScreenLayout l = screens.get(screen);
        return l == null ? ScreenLayout.EMPTY : l;
    }

    public synchronized boolean has(String screen) {
        return screens.containsKey(screen);
    }

    public void put(String screen, ScreenLayout layout) {
        synchronized (this) {
            if (layout == null || layout.isEmpty()) screens.remove(screen);
            else screens.put(screen, layout);
        }
        onChange.run();
    }

    public void remove(String screen) {
        put(screen, null);
    }

    public void clear() {
        synchronized (this) {
            screens.clear();
        }
        onChange.run();
    }

    public synchronized List<String> names() {
        return new ArrayList<>(screens.keySet());
    }

    public synchronized Map<String, ScreenLayout> snapshot() {
        return new LinkedHashMap<>(screens);
    }

    public synchronized void load(Map<String, ScreenLayout> all) {
        screens.clear();
        all.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) screens.put(k, v);
        });
    }

    public void onChange(Runnable r) {
        onChange = r == null ? () -> {} : r;
    }
}
