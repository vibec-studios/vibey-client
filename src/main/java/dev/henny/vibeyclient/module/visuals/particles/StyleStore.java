package dev.henny.vibeyclient.module.visuals.particles;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import dev.henny.vibeyclient.module.visuals.effects.EffectRules;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyles;
import dev.henny.vibeyclient.module.visuals.particles.core.StyleCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;

public final class StyleStore {
    static final String DATA_KEY = "particleStyles";
    private static final ParticleStyles STYLES = new ParticleStyles();
    private static final Map<ParticleType<?>, StyleSlot> SLOTS = new IdentityHashMap<>();
    private static SettingsGroup group;
    private static boolean loaded;
    private static int seenRevision = -1;
    private static int seenRules = -1;

    private StyleStore() {}

    public static void attach(SettingsGroup settings) {
        group = settings;
        loaded = false;
        ServerRules.listen(StyleStore::refresh);
    }

    public static ParticleStyles styles() {
        load();
        return STYLES;
    }

    public static void set(String id, ParticleStyle style) {
        styles().set(id, style);
        save();
        refresh();
    }

    public static void replaceAll(Map<String, ParticleStyle> next) {
        styles().replaceAll(next);
        save();
        refresh();
    }

    public static StyleSlot slot(ParticleType<?> type) {
        refresh();
        StyleSlot s = SLOTS.get(type);
        if (s != null) return s;
        Identifier key = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        String id = key == null ? "?" : key.toString();
        s = new StyleSlot(id, ParticleGate.category(type));
        s.style(effective(id));
        SLOTS.put(type, s);
        return s;
    }

    public static void refresh() {
        load();
        int rev = STYLES.revision(), rules = EffectRules.changes();
        if (rev == seenRevision && rules == seenRules) return;
        seenRevision = rev;
        seenRules = rules;
        for (StyleSlot s : SLOTS.values()) s.style(effective(s.id()));
    }

    private static ParticleStyle effective(String id) {
        ParticleStyle s = STYLES.get(id);
        return EffectRules.blocked(EffectRules.PARTICLES) ? s.fair() : s;
    }

    private static void load() {
        if (loaded || group == null || !group.isLoaded()) return;
        loaded = true;
        STYLES.replaceAll(StyleCodec.read(group.data().get(DATA_KEY)));
    }

    private static void save() {
        if (group == null) return;
        group.data().add(DATA_KEY, StyleCodec.write(STYLES));
        group.markDirty();
    }
}
