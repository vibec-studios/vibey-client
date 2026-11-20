package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.vui.menu.Connection;

import java.util.Locale;

public final class CornerEntry {
    private final MenuEntries.Entry entry;

    CornerEntry(MenuEntries.Entry entry) {
        this.entry = entry;
    }

    public String getId() { return entry.id(); }
    public String getIcon() { return icon(entry.icon()); }
    public String getLabel() { return Lang.tr(entry.labelKey()); }
    public int getBadge() { return MenuEntries.badge(entry); }
    public String getDot() { return "friends".equals(entry.id()) ? Connection.status() : ""; }

    MenuEntries.Entry entry() {
        return entry;
    }

    static String icon(String name) {
        if (name == null || name.isEmpty()) return "seal";
        String n = name.toLowerCase(Locale.ROOT);
        return switch (n) {
            case "hud_library" -> "hud";
            default -> n;
        };
    }
}
