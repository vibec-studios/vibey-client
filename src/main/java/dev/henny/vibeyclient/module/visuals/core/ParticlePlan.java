package dev.henny.vibeyclient.module.visuals.core;

import java.util.EnumMap;

public final class ParticlePlan {
    public static final int MAX_PERCENT = 300;
    private final EnumMap<ParticleCategory, ParticleBudget> budgets = new EnumMap<>(ParticleCategory.class);

    public ParticlePlan() {
        for (ParticleCategory c : ParticleCategory.values()) budgets.put(c, new ParticleBudget());
    }

    public int count(ParticleCategory c, int percent, boolean keepImportant, boolean ambientOff) {
        if (ambientOff && c.ambient()) return 0;
        double multiplier = Math.max(0, Math.min(MAX_PERCENT, percent)) / 100.0;
        if (c == ParticleCategory.IMPORTANT && keepImportant && multiplier < 1) return 1;
        return budgets.get(c).next(multiplier);
    }
}
