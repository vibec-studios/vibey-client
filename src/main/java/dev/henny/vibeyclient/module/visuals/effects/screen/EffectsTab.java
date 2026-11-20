package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPart;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPreview;
import dev.henny.vibeyclient.module.visuals.effects.PvpLink;
import dev.henny.vibeyclient.ui.vui.settings.Rows;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;
import dev.henny.vui.bind.ListState;

import java.util.ArrayList;
import java.util.List;

final class EffectsTab {
    private static final String[] FIRE_PVP = {"lowFire", "fireHeight"};
    private static final String[] COMBAT_PVP = {"hitColor", "hitTint", "hitStrength", "outline", "outlineColor",
            "outlineWidth", "outlineOpacity", "outlineFill", "outlineFillOpacity", "lowShield", "shieldHeight"};

    final ListState<EffectGroup> groups = new ListState<>();
    private final EffectsPart part;
    private String shape = "";

    EffectsTab(EffectsPart part) {
        this.part = part;
        sync();
    }

    void sync() {
        String next = shape();
        if (!next.equals(shape)) {
            shape = next;
            groups.replaceAll(build());
            return;
        }
        for (EffectGroup g : groups.snapshot()) for (EffectItem i : g.items()) i.row.sync();
    }

    private List<EffectGroup> build() {
        List<EffectGroup> out = new ArrayList<>();
        List<EffectItem> overlays = new ArrayList<>();
        add(overlays, part.fireOpacity, EffectsPreview.FIRE);
        for (String k : FIRE_PVP) add(overlays, PvpLink.setting(k), EffectsPreview.FIRE);
        add(overlays, part.pumpkinOpacity, EffectsPreview.PUMPKIN);
        add(overlays, part.vignette, "");
        add(overlays, part.totemSize, EffectsPreview.TOTEM);
        add(overlays, part.distortion, EffectsPreview.NAUSEA);
        out.add(group("overlays", "", overlays));
        List<EffectItem> camera = new ArrayList<>();
        add(camera, part.hurtShake, EffectsPreview.HURT);
        add(camera, part.explosionShake, EffectsPreview.BLAST);
        out.add(group("camera", "", camera));
        List<EffectItem> combat = new ArrayList<>();
        for (String k : COMBAT_PVP) add(combat, PvpLink.setting(k), "");
        if (!combat.isEmpty()) out.add(group("combat", PvpLink.on() ? "" : Lang.tr(EffectsPart.PREFIX + "ui.pvpOff"), combat));
        List<EffectItem> self = new ArrayList<>();
        add(self, part.ownPotionParticles, "");
        out.add(group("self", "", self));
        return out;
    }

    private static EffectGroup group(String id, String note, List<EffectItem> items) {
        return new EffectGroup(id, Lang.tr(EffectsPart.PREFIX + "section." + id), note, items);
    }

    private static void add(List<EffectItem> into, Setting<?, ?> s, String test) {
        if (s == null || !s.isVisible()) return;
        SettingRow row = Rows.of(s);
        if (row != null) into.add(new EffectItem(row, test));
    }

    private String shape() {
        StringBuilder b = new StringBuilder(PvpLink.on() ? "p" : "-");
        for (Setting<?, ?> s : part.effectSettings()) b.append(s.isVisible() ? '1' : '0');
        for (String k : FIRE_PVP) b.append(visible(k));
        for (String k : COMBAT_PVP) b.append(visible(k));
        return b.toString();
    }

    private static char visible(String key) {
        Setting<?, ?> s = PvpLink.setting(key);
        return s == null ? 'x' : s.isVisible() ? '1' : '0';
    }
}
