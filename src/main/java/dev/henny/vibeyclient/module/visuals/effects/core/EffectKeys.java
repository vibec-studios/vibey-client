package dev.henny.vibeyclient.module.visuals.effects.core;

import java.util.List;

public final class EffectKeys {
    public static final String FIRE = "fireOpacity";
    public static final String HURT = "hurtShake";
    public static final String TOTEM = "totemSize";
    public static final String DISTORTION = "distortion";
    public static final String PUMPKIN = "pumpkinOpacity";
    public static final String VIGNETTE = "vignette";
    public static final String BLAST = "explosionShake";
    public static final String OWN_POTION = "ownPotionParticles";
    public static final List<String> PERCENTS = List.of(FIRE, HURT, TOTEM, DISTORTION, PUMPKIN, VIGNETTE, BLAST);

    private EffectKeys() {}

    public static int vanilla(String key) {
        return switch (key) {
            case BLAST -> 0;
            case OWN_POTION -> 1;
            default -> 100;
        };
    }
}
