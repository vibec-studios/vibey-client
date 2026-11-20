package dev.henny.vibeyclient.module.inventory.core;

import java.util.List;

public final class PresetPlanner {
    public static final int INVENTORY = 36;

    public record Swap(int from, int hotbar) {}

    private PresetPlanner() {}

    public static Swap next(List<ItemRef> inventory, HotbarPreset preset) {
        for (int h = 0; h < HotbarPreset.SIZE; h++) {
            ItemRef want = preset.get(h);
            if (want.empty() || inventory.get(h).exact(want)) continue;
            int from = find(inventory, preset, want, h, true);
            if (from < 0 && !inventory.get(h).sameItem(want)) from = find(inventory, preset, want, h, false);
            if (from >= 0) return new Swap(from, h);
        }
        return null;
    }

    public static boolean done(List<ItemRef> inventory, HotbarPreset preset) {
        return next(inventory, preset) == null;
    }

    private static int find(List<ItemRef> inventory, HotbarPreset preset, ItemRef want, int hotbar, boolean exact) {
        for (int i = HotbarPreset.SIZE; i < INVENTORY; i++) if (fits(inventory.get(i), want, exact)) return i;
        for (int k = 0; k < HotbarPreset.SIZE; k++) {
            if (k == hotbar || satisfied(inventory, preset, k)) continue;
            if (fits(inventory.get(k), want, exact)) return k;
        }
        return -1;
    }

    private static boolean satisfied(List<ItemRef> inventory, HotbarPreset preset, int k) {
        ItemRef want = preset.get(k);
        return !want.empty() && inventory.get(k).sameItem(want);
    }

    private static boolean fits(ItemRef have, ItemRef want, boolean exact) {
        return exact ? have.exact(want) : have.sameItem(want);
    }
}
