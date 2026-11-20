package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.afk.AfkLayout;
import dev.henny.vibeyclient.ui.hud.HudLayout;

final class HudSwap {
    private static String before;

    private HudSwap() {}

    static void enter() {
        if (before != null) return;
        before = HudLayout.profile();
        HudLayout.useProfile(AfkLayout.PROFILE);
    }

    static void leave() {
        if (before == null) return;
        String b = before;
        before = null;
        HudLayout.useProfile(b);
    }
}
