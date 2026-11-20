package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.module.visuals.core.ParticleCategory;
import dev.henny.vibeyclient.module.visuals.core.ParticleTable;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPart;
import dev.henny.vibeyclient.module.visuals.particles.ParticleIds;
import dev.henny.vibeyclient.module.visuals.particles.StyleStore;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleNames;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyles;
import dev.henny.vibeyclient.ui.vui.WatchedState;
import dev.henny.vibeyclient.ui.vui.settings.Rows;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ParticlesTab {
    static final String ALL = "all", CHANGED = "changed";
    private static final String[] GLOBALS = {"particles", "keepImportant", "ambientOff"};

    final WatchedState<String> query = new WatchedState<>("");
    final WatchedState<String> filter = new WatchedState<>(ALL);
    final ListState<ParticleItem> rows = new ListState<>();
    final ListState<SettingRow> globals = new ListState<>();
    final State<ParticleDetail> selected = new State<>(null);
    private int revision = -1;

    ParticlesTab(SettingsGroup visuals) {
        for (String key : GLOBALS) {
            Setting<?, ?> s = visuals.get(key);
            SettingRow row = s == null ? null : Rows.of(s);
            if (row != null) globals.add(row);
        }
        query.onChange(q -> rebuild());
        filter.onChange(f -> rebuild());
        sync();
    }

    void sync() {
        if (StyleStore.styles().revision() != revision) rebuild();
        for (SettingRow r : globals.snapshot()) r.sync();
        ParticleDetail d = selected.peek();
        if (d != null) d.sync();
    }

    void select(ParticleItem item) {
        if (item == null || item.header) return;
        ParticleDetail d = new ParticleDetail(item.id);
        selected.set(d);
        d.burst();
    }

    void back() {
        selected.set(null);
    }

    void toggleHidden(ParticleItem item) {
        if (item == null || item.header) return;
        ParticleStyle s = StyleStore.styles().get(item.id);
        StyleStore.set(item.id, s.withHidden(!s.hidden()));
    }

    int changed() {
        return StyleStore.styles().changed();
    }

    private void rebuild() {
        ParticleStyles styles = StyleStore.styles();
        revision = styles.revision();
        String f = filter.peek(), q = query.peek();
        List<ParticleItem> out = new ArrayList<>();
        for (ParticleCategory c : ParticleCategory.values()) {
            if (!f.equals(ALL) && !f.equals(CHANGED) && !f.equals(c.name().toLowerCase(Locale.ROOT))) continue;
            List<ParticleItem> group = new ArrayList<>();
            for (String id : ParticleIds.all()) {
                if (ParticleTable.category(id) != c) continue;
                ParticleStyle s = styles.get(id);
                if (f.equals(CHANGED) && s.vanilla()) continue;
                if (!ParticleNames.matches(id, ParticleNames.pretty(id), q)) continue;
                group.add(ParticleItem.of(id, s));
            }
            if (group.isEmpty()) continue;
            out.add(ParticleItem.header(c));
            out.addAll(group);
        }
        rows.replaceAll(out);
    }

    List<Map<String, Object>> filters() {
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(option(ALL));
        for (ParticleCategory c : ParticleCategory.values()) out.add(option(c.name().toLowerCase(Locale.ROOT)));
        out.add(option(CHANGED));
        return out;
    }

    private static Map<String, Object> option(String id) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("value", id);
        o.put("label", Lang.tr(EffectsPart.PREFIX + "ui.cat." + id));
        return o;
    }
}
