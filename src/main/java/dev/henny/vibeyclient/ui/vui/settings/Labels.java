package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;

public final class Labels {
    private Labels() {}

    public static String label(Setting<?, ?> s) {
        return Lang.tr(s.nameKey());
    }

    public static String tip(Setting<?, ?> s) {
        return Lang.has(s.descriptionKey()) ? Lang.tr(s.descriptionKey()) : "";
    }

    public static String button(ActionSetting a) {
        String k = a.nameKey() + ".button";
        return Lang.has(k) ? Lang.tr(k) : Lang.tr(a.nameKey());
    }

    public static String action(ActionSetting a) {
        String k = a.nameKey() + ".action";
        return Lang.has(k) ? Lang.tr(k) : Lang.tr(a.nameKey());
    }
}
