package dev.henny.cosmetico.state;

import dev.henny.cosmetico.model.Slot;

import java.util.Arrays;

public final class LoadoutLayers {
    public final String[] base = new String[Slot.count()];
    public final String[] overrides = new String[Slot.count()];
    public final boolean[] overridden = new boolean[Slot.count()];

    public String shown(Slot slot) {
        int i = slot.ordinal();
        return overridden[i] ? overrides[i] : base[i];
    }

    public void override(Slot slot, String itemId) {
        overridden[slot.ordinal()] = true;
        overrides[slot.ordinal()] = itemId;
    }

    public void clearOverrides() {
        Arrays.fill(overridden, false);
        Arrays.fill(overrides, null);
    }

    public void clearBase() {
        Arrays.fill(base, null);
    }
}
