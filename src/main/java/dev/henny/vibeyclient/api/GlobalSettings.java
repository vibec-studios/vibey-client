package dev.henny.vibeyclient.api;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.client.Keys;

/** Settings that apply to the whole client ({@code config/vibeyclient/core.json}), shown as "Global settings". */
public final class GlobalSettings {
    private GlobalSettings() {}

    public enum Language { AUTO, GERMAN, ENGLISH }

    public static final SettingsGroup GROUP = new SettingsGroup("core");

    public static final EnumSetting<Language> language = GROUP.add(new EnumSetting<>("language", Language.AUTO));

    /** Off: no backend connection at all, social hidden, cosmetics only local / via server packets. */
    public static final BoolSetting onlineFeatures = GROUP.add(new BoolSetting("onlineFeatures", true))
            .onChange(on -> Events.post(new VibeyEvents.OnlineFeaturesChanged(on)));

    public static final BoolSetting animations = GROUP.add(new BoolSetting("animations", true));

    public static final KeybindSetting openMenu = GROUP.add(new KeybindSetting("core", "open_menu", Keys.RIGHT_SHIFT));

    public static final StringSetting backendUrl = GROUP.add(new StringSetting("backendUrl", BuildInfo.DEFAULT_BACKEND, 256)).advanced();

    public static final ActionSetting resetBackend = GROUP.add(new ActionSetting("resetBackend",
            () -> GlobalSettings.backendUrl.reset())).advanced();

    /** Backend base URL without a trailing slash. */
    public static String backend() {
        String s = backendUrl.get().trim();
        if (s.isEmpty()) s = BuildInfo.DEFAULT_BACKEND;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static boolean online() {
        return onlineFeatures.on();
    }

    public static boolean animate() {
        return animations.on();
    }

    public static void load() {
        GROUP.load();
    }
}
