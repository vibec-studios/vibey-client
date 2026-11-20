package dev.henny.vibeyclient.module.visuals.effects.core;

import java.util.LinkedHashMap;
import java.util.Map;

import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.BLAST;
import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.DISTORTION;
import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.FIRE;
import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.HURT;
import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.OWN_POTION;
import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.PUMPKIN;
import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.TOTEM;
import static dev.henny.vibeyclient.module.visuals.effects.core.EffectKeys.VIGNETTE;

public final class EffectPresets {
    private EffectPresets() {}

    public static Map<String, Integer> values(Preset p) {
        Map<String, Integer> v = new LinkedHashMap<>();
        for (String k : EffectKeys.PERCENTS) v.put(k, EffectKeys.vanilla(k));
        v.put(OWN_POTION, EffectKeys.vanilla(OWN_POTION));
        switch (p) {
            case CLEAN -> {
                v.put(VIGNETTE, 40);
                v.put(HURT, 60);
                v.put(DISTORTION, 50);
            }
            case PVP -> {
                v.put(FIRE, 60);
                v.put(HURT, 40);
                v.put(TOTEM, 60);
                v.put(PUMPKIN, 60);
                v.put(VIGNETTE, 50);
                v.put(DISTORTION, 40);
                v.put(OWN_POTION, 0);
            }
            case FANCY -> {
                v.put(TOTEM, 120);
                v.put(BLAST, 60);
            }
            default -> {
            }
        }
        return v;
    }
}
