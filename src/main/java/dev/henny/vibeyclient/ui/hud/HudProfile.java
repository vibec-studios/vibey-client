package dev.henny.vibeyclient.ui.hud;

import java.util.Set;

public record HudProfile(String id, String file, Set<String> shown) {
    public static final String MAIN = "main";

    boolean hiddenByDefault(HudElement e) {
        return shown == null ? e.startHidden() : !shown.contains(e.baseId());
    }

    boolean hidesUnlisted(HudElement e) {
        return shown != null || e.isWidget() && e.startHidden();
    }

    boolean main() {
        return shown == null;
    }
}
