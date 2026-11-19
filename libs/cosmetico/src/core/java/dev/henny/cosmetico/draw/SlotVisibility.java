package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.config.CosmeticoConfig;

public final class SlotVisibility {
    private SlotVisibility() {}

    public static boolean hidden(CosmeticoConfig config, boolean self, boolean invisible, boolean spectator) {
        return !config.cosmetics || invisible || spectator || (self ? !config.showOwn : !config.showOthers);
    }
}
