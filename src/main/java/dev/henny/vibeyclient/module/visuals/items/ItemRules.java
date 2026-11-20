package dev.henny.vibeyclient.module.visuals.items;

import dev.henny.vibeyclient.api.ServerRules;

public final class ItemRules {
    public static final String HELD = "visuals.helditem";

    private ItemRules() {}

    public static void declare() {
        ServerRules.supports(HELD);
    }

    public static boolean heldBlocked() {
        return ServerRules.isDisabled(HELD);
    }
}
