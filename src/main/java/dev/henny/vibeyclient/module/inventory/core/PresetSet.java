package dev.henny.vibeyclient.module.inventory.core;

import java.util.ArrayList;
import java.util.List;

public final class PresetSet {
    public static final int MIN = 2, MAX = 6;

    private final List<HotbarPreset> presets = new ArrayList<>();
    private int count;
    private int active;

    public PresetSet(int count) {
        while (presets.size() < MAX) presets.add(HotbarPreset.BLANK);
        resize(count);
    }

    public void resize(int count) {
        this.count = Math.max(MIN, Math.min(MAX, count));
        active = Math.min(active, this.count - 1);
    }

    public int size() {
        return count;
    }

    public int active() {
        return active;
    }

    public HotbarPreset get(int i) {
        return presets.get(i);
    }

    public List<HotbarPreset> all() {
        return List.copyOf(presets.subList(0, count));
    }

    public List<HotbarPreset> stored() {
        return List.copyOf(presets);
    }

    public void put(int i, HotbarPreset preset) {
        if (i >= 0 && i < presets.size()) presets.set(i, preset == null ? HotbarPreset.BLANK : preset);
    }

    public void select(int i) {
        active = Math.floorMod(i, count);
    }

    public int next() {
        return Math.floorMod(active + 1, count);
    }

    public int step(int from, int delta) {
        return Math.floorMod(from + delta, count);
    }
}
