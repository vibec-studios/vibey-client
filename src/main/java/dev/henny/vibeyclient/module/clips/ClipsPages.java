package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.vui.Menus;

/** Opens the module's settings page in the Vibey menu. */
public final class ClipsPages {
    private ClipsPages() {}

    public static void openSettings() {
        Menus.openModule(ClientCompat.screen(), "clips");
    }
}
