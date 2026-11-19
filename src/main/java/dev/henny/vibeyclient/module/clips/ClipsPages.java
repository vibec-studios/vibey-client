package dev.henny.vibeyclient.module.clips;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;

/** Opens the module's settings page in the Vibey menu. */
public final class ClipsPages {
    private ClipsPages() {}

    public static void openSettings() {
        ModulesPanel.preselect("clips");
        ClientCompat.openScreen(new VibeyScreen(ClientCompat.screen()));
    }
}
