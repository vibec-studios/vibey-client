package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.ColorSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;
import dev.henny.vibeyclient.client.MappingKeys;
import dev.henny.vui.expr.Values;

import java.util.ArrayList;
import java.util.List;

final class OtherRows {
    private OtherRows() {}

    static SettingRow of(BoolSetting s) {
        return new SettingRow(s, "toggle", Labels.label(s), Labels.tip(s), new SettingValue(s::get, o -> s.set(Values.truthy(o))));
    }

    static SettingRow of(StringSetting s) {
        SettingRow r = new SettingRow(s, "text", Labels.label(s), Labels.tip(s), new SettingValue(s::get, o -> s.set(String.valueOf(o))));
        r.placeholder = s.defaultValue();
        r.maxLength = s.maxLength();
        return r;
    }

    static SettingRow of(ColorSetting s) {
        SettingRow r = new SettingRow(s, "color", Labels.label(s), Labels.tip(s),
                new SettingValue(s::get, o -> s.set((int) (long) Values.toNumber(o))));
        List<Integer> presets = new ArrayList<>();
        for (int p : s.presets()) presets.add(p);
        r.presets = List.copyOf(presets);
        return r;
    }

    static SettingRow of(KeybindSetting s) {
        SettingRow r = new SettingRow(s, "key", Labels.label(s), Labels.tip(s),
                new SettingValue(() -> MappingKeys.value(s.mapping()), o -> MappingKeys.set(s.mapping(), (int) Values.toNumber(o))));
        r.conflict = () -> MappingKeys.conflicts(s.mapping());
        return r;
    }

    static SettingRow of(ActionSetting s) {
        SettingRow r = new SettingRow(s, "action", Labels.label(s), Labels.tip(s), new SettingValue(() -> false, o -> {
        }));
        r.button = Labels.button(s);
        return r;
    }
}
