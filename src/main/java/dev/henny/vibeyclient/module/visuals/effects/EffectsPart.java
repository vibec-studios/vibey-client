package dev.henny.vibeyclient.module.visuals.effects;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;
import dev.henny.vibeyclient.api.setting.Settings.BoolSetting;
import dev.henny.vibeyclient.api.setting.Settings.IntSetting;
import dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys;
import dev.henny.vibeyclient.module.visuals.effects.screen.EffectsScreen;
import dev.henny.vibeyclient.module.visuals.particles.StyleStore;

import java.util.List;

public final class EffectsPart {
    public static final String I18N = "visuals.effects";
    public static final String PREFIX = I18N + ".";
    private static final String SECTION = "effects";
    private static volatile EffectsPart instance;

    private final VibeyModule module;
    public final ActionSetting editor;
    public final IntSetting fireOpacity;
    public final IntSetting pumpkinOpacity;
    public final IntSetting vignette;
    public final IntSetting totemSize;
    public final IntSetting distortion;
    public final IntSetting hurtShake;
    public final IntSetting explosionShake;
    public final BoolSetting ownPotionParticles;

    public EffectsPart(VibeyModule module) {
        this.module = module;
        instance = this;
        Lang.addFile(I18N);
        editor = add(new ActionSetting("editEffects", EffectsScreen::openFromMenu));
        fireOpacity = add(percent(EffectKeys.FIRE, 100, 10)).serverRule(EffectRules.FIRE);
        pumpkinOpacity = add(percent(EffectKeys.PUMPKIN, 100, 10)).serverRule(EffectRules.PUMPKIN);
        vignette = add(percent(EffectKeys.VIGNETTE, 100, 10));
        totemSize = add(new IntSetting(EffectKeys.TOTEM, 100, 0, 150, 10).unit("%")).serverRule(EffectRules.TOTEM);
        distortion = add(percent(EffectKeys.DISTORTION, 100, 10));
        hurtShake = add(percent(EffectKeys.HURT, 100, 10));
        explosionShake = add(percent(EffectKeys.BLAST, 0, 10));
        ownPotionParticles = add(new BoolSetting(EffectKeys.OWN_POTION, true)).serverRule(EffectRules.PARTICLES);
        EffectRules.declare();
        StyleStore.attach(module.settings());
    }

    public static EffectsPart get() {
        return instance;
    }

    public boolean active() {
        return module.isEnabled();
    }

    public VibeyModule module() {
        return module;
    }

    public List<Setting<?, ?>> effectSettings() {
        return List.of(fireOpacity, pumpkinOpacity, vignette, totemSize, distortion, hurtShake, explosionShake,
                ownPotionParticles);
    }

    public Setting<?, ?> byKey(String key) {
        for (Setting<?, ?> s : effectSettings()) if (s.key().equals(key)) return s;
        return null;
    }

    private static IntSetting percent(String key, int def, int step) {
        return new IntSetting(key, def, 0, 100, step).unit("%");
    }

    private <S extends Setting<?, ?>> S add(S s) {
        module.settings().add(s);
        s.labelKey(PREFIX + "setting." + s.key());
        s.section(SECTION).sectionLabel(PREFIX + "section.effects");
        return s;
    }
}
