package dev.henny.vibeyclient.ui.vui.theme;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.dev.DevDriver;

import java.util.Locale;

public final class DevThemeCommands {
    private DevThemeCommands() {}

    public static void register() {
        DevDriver.EXTRA.put("theme", arg -> {
            GlobalSettings.theme.set(GlobalSettings.UiTheme.valueOf(arg.trim().toUpperCase(Locale.ROOT)));
            return "ok " + ThemeAudit.failures(UiThemes.active());
        });
        DevDriver.EXTRA.put("accent", arg -> {
            boolean off = arg.isBlank() || arg.trim().equals("off");
            if (!off) GlobalSettings.accent.set(Contrast.parse(arg.trim()));
            GlobalSettings.ownAccent.set(!off);
            return "ok " + ThemeAudit.failures(UiThemes.active());
        });
    }
}
