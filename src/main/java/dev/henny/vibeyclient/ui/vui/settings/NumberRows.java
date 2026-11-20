package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.setting.Settings.FloatSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vui.expr.Values;

import java.util.Locale;

final class NumberRows {
    private NumberRows() {}

    static SettingRow of(IntSetting s) {
        if (s.isField()) return field(s);
        SettingValue v = new SettingValue(s::get, o -> s.set((int) Math.round(Values.toNumber(o))));
        SettingRow r = new SettingRow(s, "slider", Labels.label(s), Labels.tip(s), v);
        r.min = s.min();
        r.max = s.max();
        r.step = s.step();
        r.format = o -> Math.round(Values.toNumber(o)) + s.unit().trim();
        return r;
    }

    static SettingRow of(FloatSetting s) {
        SettingValue v = new SettingValue(s::get, o -> s.set((float) Values.toNumber(o)));
        SettingRow r = new SettingRow(s, "slider", Labels.label(s), Labels.tip(s), v);
        r.min = s.min();
        r.max = s.max();
        r.step = s.step() > 0 ? s.step() : 0.01f;
        boolean percent = s.min() == 0f && s.max() == 1f;
        r.format = o -> percent ? Math.round(Values.toNumber(o) * 100) + "%"
                : String.format(Locale.ROOT, "%." + s.decimals() + "f", Values.toNumber(o));
        return r;
    }

    private static SettingRow field(IntSetting s) {
        SettingValue v = new SettingValue(() -> String.valueOf(s.get()), o -> {
            try {
                int n = Integer.parseInt(String.valueOf(o).trim());
                if (n >= s.min() && n <= s.max()) s.set(n);
            } catch (NumberFormatException ignored) {
            }
        });
        SettingRow r = new SettingRow(s, "text", Labels.label(s), Labels.tip(s), v);
        r.placeholder = String.valueOf(s.defaultValue());
        r.maxLength = 11;
        return r;
    }
}
