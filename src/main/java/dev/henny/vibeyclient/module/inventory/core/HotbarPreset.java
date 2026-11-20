package dev.henny.vibeyclient.module.inventory.core;

import java.util.ArrayList;
import java.util.List;

public record HotbarPreset(List<ItemRef> slots) {
    public static final int SIZE = 9;
    public static final HotbarPreset BLANK = new HotbarPreset(List.of());

    public HotbarPreset {
        List<ItemRef> fixed = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            ItemRef r = slots != null && i < slots.size() ? slots.get(i) : null;
            fixed.add(r == null ? ItemRef.EMPTY : r);
        }
        slots = List.copyOf(fixed);
    }

    public ItemRef get(int i) {
        return slots.get(i);
    }

    public boolean blank() {
        for (ItemRef r : slots) if (!r.empty()) return false;
        return true;
    }
}
