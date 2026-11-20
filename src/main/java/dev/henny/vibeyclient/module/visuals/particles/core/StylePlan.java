package dev.henny.vibeyclient.module.visuals.particles.core;

import dev.henny.vibeyclient.module.visuals.core.ParticleBudget;
import dev.henny.vibeyclient.module.visuals.core.ParticleCategory;

public final class StylePlan {
    public static final double MAX_MULTIPLIER = 6;
    public static final int MAX_GLOBAL = 300;

    private StylePlan() {}

    public static int count(ParticleCategory c, int globalPercent, boolean keepImportant, boolean ambientOff,
                            ParticleStyle style, ParticleBudget budget) {
        if (style.hidden()) return 0;
        if (ambientOff && c.ambient()) return 0;
        double global = Math.max(0, Math.min(MAX_GLOBAL, globalPercent)) / 100.0;
        if (c == ParticleCategory.IMPORTANT && keepImportant) global = Math.max(1, global);
        double m = Math.min(MAX_MULTIPLIER, global * style.amount() / 100.0);
        return budget.next(m);
    }

    public static boolean neutral(int globalPercent, boolean ambientOff, ParticleStyle style) {
        return globalPercent == 100 && !ambientOff && style.amount() == 100 && !style.hidden();
    }
}
