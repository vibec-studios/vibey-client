package dev.henny.vibeyclient.module.inventory.core;

import java.util.ArrayList;
import java.util.List;

public final class PresetSet {
    public static final int MIN = 2, MAX = 6;

    private final List<HotbarPreset> presets = new ArrayList<>();
    private int active;

    public PresetSet(int count) {
        resize(count);
    }

    public void resize(int count) {
        int n = Math.max(MIN, Math.min(MAX, count));
        while (presets.size() < n) presets.add(HotbarPreset.BLANK);
        while (presets.size() > n) presets.remove(presets.size() - 1);
        active = Math.min(active, n - 1);
    }

    public int size() {
        return presets.size();
    }

    public int active() {
        return active;
    }

    public HotbarPreset get(int i) {
        return presets.get(i);
    }

    public List<HotbarPreset> all() {
        return List.copyOf(presets);
    }

    public void put(int i, HotbarPreset preset) {
        if (i >= 0 && i < presets.size()) presets.set(i, preset == null ? HotbarPreset.BLANK : preset);
    }

    public void select(int i) {
        active = Math.floorMod(i, presets.size());
    }

    public int next() {
        return Math.floorMod(active + 1, presets.size());
    }

    public int step(int from, int delta) {
        return Math.floorMod(from + delta, presets.size());
    }
}
