package dev.henny.vibeyclient.module.cosmetics;

import dev.henny.cosmetico.model.Slot;

import java.util.Arrays;
import java.util.List;

public final class SlotRules {
    private SlotRules() {}

    public static final List<String> KEYS = Arrays.stream(Slot.all()).map(SlotRules::key).toList();

    public static String key(Slot slot) {
        return "cosmetics." + slot.id() + (slot == Slot.WINGS ? "" : "s");
    }
}
