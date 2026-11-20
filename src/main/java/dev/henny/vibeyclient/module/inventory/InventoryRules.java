package dev.henny.vibeyclient.module.inventory;

import java.util.List;

public final class InventoryRules {
    public static final String TWEAKS = "inventory.tweaks", PRESETS = "inventory.presets";
    public static final List<String> ALL = List.of(TWEAKS, PRESETS);

    private InventoryRules() {}
}
