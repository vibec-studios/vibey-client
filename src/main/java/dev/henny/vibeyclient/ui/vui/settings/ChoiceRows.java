package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Settings.EnumSetting;

import java.util.ArrayList;
import java.util.List;

final class ChoiceRows {
    private ChoiceRows() {}

    static final int COMPACT_OPTIONS = 4;
    static final int COMPACT_CHARS = 6;

    static <E extends Enum<E>> SettingRow of(EnumSetting<E> s) {
        SettingValue v = new SettingValue(() -> s.get().name(), o -> s.set(parse(s, String.valueOf(o))));
        SettingRow r = new SettingRow(s, "choice", Labels.label(s), Labels.tip(s), v);
        List<Option> options = new ArrayList<>();
        boolean compact = s.values().length <= COMPACT_OPTIONS;
        for (E e : s.values()) {
            String label = Lang.tr(s.valueKey(e));
            options.add(new Option(e.name(), label));
            if (label.length() > COMPACT_CHARS) compact = false;
        }
        r.options = List.copyOf(options);
        r.compact = compact;
        r.format = o -> labelOf(r.options, String.valueOf(o));
        return r;
    }

    private static <E extends Enum<E>> E parse(EnumSetting<E> s, String name) {
        for (E e : s.values()) if (e.name().equalsIgnoreCase(name)) return e;
        return s.get();
    }

    private static String labelOf(List<Option> options, String value) {
        for (Option o : options) if (o.value().equals(value)) return o.label();
        return value;
    }
}
