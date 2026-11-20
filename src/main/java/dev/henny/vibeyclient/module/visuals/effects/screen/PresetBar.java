package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPart;
import dev.henny.vibeyclient.module.visuals.effects.core.EffectPresets;
import dev.henny.vibeyclient.module.visuals.effects.core.Preset;
import dev.henny.vibeyclient.module.visuals.particles.ParticleIds;
import dev.henny.vibeyclient.module.visuals.particles.StyleStore;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticlePresets;
import dev.henny.vibeyclient.ui.vui.WatchedState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PresetBar {
    final WatchedState<String> preset = new WatchedState<>("");
    private final EffectsPart part;

    PresetBar(EffectsPart part) {
        this.part = part;
        preset.onChange(id -> {
            Preset p = Preset.of(id);
            if (p != null) apply(p);
        });
        sync();
    }

    List<Map<String, Object>> options() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Preset p : Preset.values()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("value", p.id());
            o.put("label", Lang.tr(EffectsPart.PREFIX + "ui.preset." + p.id()));
            out.add(o);
        }
        return out;
    }

    void sync() {
        preset.quiet(current());
    }

    void apply(Preset p) {
        Map<String, Integer> values = EffectPresets.values(p);
        values.forEach((key, v) -> {
            Setting<?, ?> s = part.byKey(key);
            if (s instanceof IntSetting i) i.set(v);
            else if (s instanceof BoolSetting b) b.set(v != 0);
        });
        StyleStore.replaceAll(ParticlePresets.styles(p, ParticleIds.all()));
        preset.quiet(p.id());
    }

    private String current() {
        for (Preset p : Preset.values()) if (matches(p)) return p.id();
        return "";
    }

    private boolean matches(Preset p) {
        for (Map.Entry<String, Integer> e : EffectPresets.values(p).entrySet()) {
            Setting<?, ?> s = part.byKey(e.getKey());
            if (s instanceof IntSetting i && i.get() != e.getValue().intValue()) return false;
            if (s instanceof BoolSetting b && b.on() != (e.getValue() != 0)) return false;
        }
        return ParticlePresets.matches(p, StyleStore.styles(), ParticleIds.all());
    }
}
