package dev.henny.vibeyclient.module.inventory.presets;

import dev.henny.vibeyclient.module.inventory.InventoryModule;
import dev.henny.vibeyclient.module.inventory.InventoryRules;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;

public final class PresetHud {
    public static final String ID = "inventory.presets";
    static final long SHOW_AFTER_SWITCH_MS = 1800;

    private PresetHud() {}

    public static void register(InventoryModule m) {
        HudLayout.register(HudElement.builder(ID)
                .label("inventory.hud.presets")
                .description("inventory.hud.presets.desc")
                .icon(Icons.MODULES)
                .category(HudElement.Category.OTHER)
                .rules(InventoryRules.PRESETS)
                .defaults(Anchor.BOTTOM_RIGHT, -4, -4)
                .scale(1f, 0.5f, 3f)
                .visibleWhen(m::presetsActive)
                .widget(PresetBarWidget::new)
                .build());
    }

    static boolean wanted(InventoryModule m) {
        return m.presetAlways.on() || PresetKeys.holding()
                || System.currentTimeMillis() - Presets.switchedAt() < SHOW_AFTER_SWITCH_MS;
    }
}
