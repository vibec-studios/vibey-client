package dev.henny.vibeyclient.module.visuals.effects;

import dev.henny.vibeyclient.module.visuals.effects.core.EffectMath;

public final class Effects {
    private Effects() {}

    private static EffectsPart on() {
        EffectsPart p = EffectsPart.get();
        return p != null && p.active() ? p : null;
    }

    public static float fireAlpha(float vanilla) {
        EffectsPart p = on();
        if (p == null) return vanilla;
        return EffectMath.fireAlpha(p.fireOpacity.get(), EffectRules.blocked(EffectRules.FIRE)) * (vanilla / EffectMath.FIRE_ALPHA);
    }

    public static float overlayAlpha(float vanilla) {
        EffectsPart p = on();
        if (p == null) return vanilla;
        return EffectMath.overlayAlpha(vanilla, p.pumpkinOpacity.get(), EffectRules.blocked(EffectRules.PUMPKIN));
    }

    public static float vignette(float vanilla) {
        EffectsPart p = on();
        return p == null ? vanilla : vanilla * EffectMath.scale(p.vignette.get());
    }

    public static float totemScale() {
        EffectsPart p = on();
        return p == null ? 1f : EffectMath.fair(p.totemSize.get(), EffectRules.blocked(EffectRules.TOTEM));
    }

    public static float nausea(float vanilla) {
        return Math.max(vanilla, EffectsPreview.nausea()) * distortionScale();
    }

    public static float portal(float vanilla) {
        return vanilla * distortionScale();
    }

    private static float distortionScale() {
        EffectsPart p = on();
        return p == null ? 1f : EffectMath.scale(p.distortion.get());
    }

    public static double hurtTilt(double vanilla) {
        EffectsPart p = on();
        return p == null ? vanilla : vanilla * EffectMath.scale(p.hurtShake.get());
    }

    public static float blastStrength() {
        EffectsPart p = on();
        return p == null ? 0f : EffectMath.scale(p.explosionShake.get());
    }

    public static boolean hideOwnPotionParticles() {
        EffectsPart p = on();
        return p != null && !p.ownPotionParticles.on() && !EffectRules.blocked(EffectRules.PARTICLES);
    }
}
