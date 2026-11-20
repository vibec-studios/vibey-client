package dev.henny.vibeyclient.ui.vui.onboarding;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.client.MappingKeys;
import dev.henny.vibeyclient.ui.vui.settings.SettingValue;
import dev.henny.vui.expr.Values;

public final class KeyItem {
    private final String id;
    private final String icon;
    private final String labelKey;
    private final SettingValue value;

    KeyItem(String id, String icon, String labelKey, KeybindSetting key) {
        this.id = id;
        this.icon = icon;
        this.labelKey = labelKey;
        this.value = new SettingValue(() -> MappingKeys.value(key.mapping()),
                o -> MappingKeys.set(key.mapping(), (int) Values.toNumber(o)));
    }

    public String getId() { return id; }
    public String getIcon() { return icon; }
    public String getLabel() { return Lang.tr(labelKey); }
    public SettingValue getValue() { return value; }

    void sync() {
        value.sync();
    }
}
