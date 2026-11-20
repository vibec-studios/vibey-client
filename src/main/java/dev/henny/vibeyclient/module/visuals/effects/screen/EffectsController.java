package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPart;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPreview;
import dev.henny.vibeyclient.module.visuals.effects.PvpLink;
import dev.henny.vibeyclient.module.visuals.particles.ParticleSprites;
import dev.henny.vibeyclient.module.visuals.particles.core.TintMath;
import dev.henny.vibeyclient.ui.vui.WatchedState;
import dev.henny.vibeyclient.ui.vui.settings.SettingRow;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EffectsController {
    public static final String EFFECTS = "effects", PARTICLES = "particles";

    private final EffectsTab effectsTab;
    private final ParticlesTab particlesTab;
    private final PresetBar presetBar;
    private final Runnable onClose;

    public final WatchedState<String> tab = new WatchedState<>(EFFECTS);
    public final WatchedState<String> preset;
    public final ListState<EffectGroup> groups;
    public final WatchedState<String> query;
    public final WatchedState<String> filter;
    public final ListState<ParticleItem> rows;
    public final ListState<SettingRow> globals;
    public final State<ParticleDetail> sel;
    public final State<Integer> frame = new State<>(0);
    public final List<String> tints = new ArrayList<>();

    EffectsController(EffectsPart part, Runnable onClose) {
        this.onClose = onClose;
        effectsTab = new EffectsTab(part);
        particlesTab = new ParticlesTab(part.module().settings());
        presetBar = new PresetBar(part);
        preset = presetBar.preset;
        groups = effectsTab.groups;
        query = particlesTab.query;
        filter = particlesTab.filter;
        rows = particlesTab.rows;
        globals = particlesTab.globals;
        sel = particlesTab.selected;
        for (int c : ParticleDetail.TINTS) tints.add(TintMath.hex(c));
    }

    public int getChanged() { return particlesTab.changed(); }
    public List<Map<String, Object>> getTabs() { return List.of(option(EFFECTS), option(PARTICLES)); }
    public List<Map<String, Object>> getPresets() { return presetBar.options(); }
    public List<Map<String, Object>> getFilters() { return particlesTab.filters(); }

    public String sprite(String particleId, int tick) {
        return ParticleSprites.frame(particleId, tick);
    }

    public void test(String kind) {
        EffectsPreview.run(kind);
    }

    public void run(Object row) {
        if (row instanceof SettingRow r) r.run();
        sync();
    }

    public void select(ParticleItem item) {
        particlesTab.select(item);
    }

    public void hide(ParticleItem item) {
        particlesTab.toggleHidden(item);
    }

    public void back() {
        particlesTab.back();
    }

    public void burst() {
        ParticleDetail d = sel.peek();
        if (d != null) d.burst();
    }

    public void resetSel() {
        ParticleDetail d = sel.peek();
        if (d != null) d.reset();
    }

    public void enablePvp() {
        PvpLink.enable();
        sync();
    }

    public void close() {
        onClose.run();
    }

    void sync() {
        frame.set(frame.peek() + 1);
        effectsTab.sync();
        particlesTab.sync();
        presetBar.sync();
    }

    ParticlesTab particles() {
        return particlesTab;
    }

    private static Map<String, Object> option(String id) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("value", id);
        o.put("label", Lang.tr(EffectsPart.PREFIX + "ui.tab." + id));
        return o;
    }
}
