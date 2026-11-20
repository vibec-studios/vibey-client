package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.setting.OptionSetting;

import java.util.ArrayList;
import java.util.List;

final class OptionRows {
    private OptionRows() {}

    static SettingRow of(OptionSetting s) {
        SettingRow r = new SettingRow(s, "choice", Labels.label(s), Labels.tip(s), new SettingValue(s::get, o -> s.set(String.valueOf(o))));
        List<Option> options = new ArrayList<>();
        boolean compact = s.choices().size() <= ChoiceRows.COMPACT_OPTIONS;
        for (OptionSetting.Choice c : s.choices()) {
            options.add(new Option(c.value(), c.label()));
            if (c.label().length() > ChoiceRows.COMPACT_CHARS) compact = false;
        }
        r.options = List.copyOf(options);
        r.compact = compact;
        return r;
    }
}
