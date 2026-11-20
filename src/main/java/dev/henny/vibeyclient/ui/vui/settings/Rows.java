package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.ColorSetting;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;
import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.Settings.StringSetting;

public final class Rows {
    private Rows() {}

    public static SettingRow toggle(String id, String label, String tip, java.util.function.BooleanSupplier read,
                                    java.util.function.Consumer<Boolean> write) {
        SettingValue v = new SettingValue(read::getAsBoolean, o -> write.accept(dev.henny.vui.expr.Values.truthy(o)));
        return new SettingRow(new BoolSetting(id, false), "toggle", label, tip, v);
    }

    public static SettingRow of(Setting<?, ?> s) {
        if (s instanceof BoolSetting b) return OtherRows.of(b);
        if (s instanceof IntSetting i) return NumberRows.of(i);
        if (s instanceof FloatSetting f) return NumberRows.of(f);
        if (s instanceof EnumSetting<?> e) return ChoiceRows.of(e);
        if (s instanceof StringSetting t) return OtherRows.of(t);
        if (s instanceof ColorSetting c) return OtherRows.of(c);
        if (s instanceof KeybindSetting k) return OtherRows.of(k);
        if (s instanceof ActionSetting a) return OtherRows.of(a);
        if (s instanceof dev.henny.vibeyclient.api.setting.OptionSetting o) return OptionRows.of(o);
        return null;
    }
}
