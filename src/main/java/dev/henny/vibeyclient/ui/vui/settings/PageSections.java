package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PageSections {
    private static final String KEYS = "__keys";

    private PageSections() {}

    public record Result(ActionSetting primary, List<Section> main, List<Section> more, int moreCount) {
        public List<SettingRow> allRows() {
            List<SettingRow> out = new ArrayList<>();
            for (Section s : main) out.addAll(s.rows());
            for (Section s : more) out.addAll(s.rows());
            return out;
        }
    }

    public static Result of(SettingsGroup group, String moduleName) {
        ActionSetting primary = null;
        List<Setting<?, ?>> basic = new ArrayList<>();
        List<Setting<?, ?>> advanced = new ArrayList<>();
        for (Setting<?, ?> s : group.all()) {
            if (!s.isVisible()) continue;
            if (s.isAdvanced()) advanced.add(s);
            else if (primary == null && s instanceof ActionSetting a) primary = a;
            else basic.add(s);
        }
        List<Section> more = sections(advanced, moduleName);
        int count = 0;
        for (Section s : more) count += s.rows().size();
        return new Result(primary, sections(basic, moduleName), more, count);
    }

    public static List<Section> sections(List<Setting<?, ?>> settings, String moduleName) {
        Map<String, List<Setting<?, ?>>> groups = new LinkedHashMap<>();
        List<Setting<?, ?>> looseKeys = new ArrayList<>();
        for (Setting<?, ?> s : settings) {
            if (s instanceof KeybindSetting && s.section() == null) looseKeys.add(s);
            else groups.computeIfAbsent(s.section() == null ? "" : s.section(), k -> new ArrayList<>()).add(s);
        }
        if (!looseKeys.isEmpty()) groups.computeIfAbsent(groups.containsKey("") ? "" : KEYS, k -> new ArrayList<>()).addAll(looseKeys);
        List<Section> out = new ArrayList<>();
        for (Map.Entry<String, List<Setting<?, ?>>> e : groups.entrySet()) {
            List<SettingRow> rows = new ArrayList<>();
            for (Setting<?, ?> s : e.getValue()) {
                SettingRow r = Rows.of(s);
                if (r != null) rows.add(r);
            }
            if (!rows.isEmpty()) out.add(new Section(e.getKey(), title(e.getKey(), e.getValue().get(0), moduleName), List.copyOf(rows)));
        }
        return out;
    }

    private static String title(String key, Setting<?, ?> first, String moduleName) {
        if (key.isEmpty()) return "";
        if (key.equals(KEYS)) return moduleName;
        return Lang.tr(first.sectionNameKey());
    }

    public static String visibility(SettingsGroup group) {
        StringBuilder sb = new StringBuilder();
        for (Setting<?, ?> s : group.all()) sb.append(s.isVisible() ? '1' : '0');
        return sb.toString();
    }

    public static String match(SettingsGroup group, String lowerQuery) {
        if (lowerQuery.isEmpty()) return null;
        for (Setting<?, ?> s : group.all()) {
            if (!s.isVisible()) continue;
            String name = Lang.tr(s.nameKey());
            if (name.toLowerCase(java.util.Locale.ROOT).contains(lowerQuery)) return name;
        }
        return null;
    }
}
