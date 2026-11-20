package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.net.NetLimits;
import dev.henny.vibeyclient.ui.title.BackendStatus;

import java.util.Locale;

public final class BugInfo {
    private BugInfo() {}

    public static String text() {
        return "Vibey Client " + BuildInfo.VERSION + "\n"
                + Vibey.platform().loader() + " · Minecraft " + BuildInfo.MINECRAFT_LABEL + "\n"
                + "Server: " + GlobalSettings.backend() + "\n"
                + "Connection: " + (GlobalSettings.online() ? BackendStatus.state().name().toLowerCase(Locale.ROOT) : "off") + "\n"
                + "Protocol: " + NetLimits.PROTOCOL + "\n"
                + "Config: " + SettingsGroup.configDir();
    }

    public static void copy() {
        try {
            ClientCompat.setClipboard(text());
        } catch (RuntimeException ignored) {
        }
    }
}
