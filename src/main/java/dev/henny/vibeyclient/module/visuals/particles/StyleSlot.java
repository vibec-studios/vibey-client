package dev.henny.vibeyclient.module.visuals.particles;

import dev.henny.vibeyclient.module.visuals.core.ParticleBudget;
import dev.henny.vibeyclient.module.visuals.core.ParticleCategory;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import dev.henny.vibeyclient.module.visuals.particles.core.TintMath;

public final class StyleSlot {
    private final String id;
    private final ParticleCategory category;
    private final ParticleBudget budget = new ParticleBudget();
    private volatile ParticleStyle style = ParticleStyle.VANILLA;

    StyleSlot(String id, ParticleCategory category) {
        this.id = id;
        this.category = category;
    }

    public String id() {
        return id;
    }

    public ParticleCategory category() {
        return category;
    }

    public ParticleBudget budget() {
        return budget;
    }

    public ParticleStyle style() {
        return style;
    }

    void style(ParticleStyle next) {
        style = next;
    }

    public float size(float vanilla) {
        ParticleStyle s = style;
        return s.size() == 100 ? vanilla : vanilla * s.sizeFactor();
    }

    public int color(int argb) {
        ParticleStyle s = style;
        return s.tinted() ? TintMath.apply(argb, s.tint(), s.strength()) : argb;
    }
}
