package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.MenuEntries;
import net.minecraft.client.gui.screens.Screen;

public final class Places {
    private Places() {}

    public static final String WEBSITE = "https://vibe.axolite.app";
    public static final String SOURCE = "https://github.com/vibec-studios/vibey-client";
    public static final String ISSUES = SOURCE + "/issues";
    public static final String LICENSES = SOURCE + "/tree/main/licenses";

    public static boolean entry(String id, Screen from) {
        for (MenuEntries.Place place : MenuEntries.Place.values()) {
            for (MenuEntries.Entry e : MenuEntries.visible(place)) {
                if (!e.id().equals(id)) continue;
                e.action().accept(from);
                return true;
            }
        }
        return false;
    }

    public static void hud(Screen from) {
        dev.henny.vibeyclient.ui.hud.HudLayout.openEditor(from);
    }

    public static void link(Screen from, String id) {
        String url = switch (id) {
            case "source" -> SOURCE;
            case "issues" -> ISSUES;
            case "licenses" -> LICENSES;
            default -> WEBSITE;
        };
        ClientCompat.openLink(from, url);
    }
}
