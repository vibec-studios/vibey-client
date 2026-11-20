package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.visuals.core.ParticleCategory;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPart;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleNames;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import dev.henny.vibeyclient.module.visuals.particles.core.TintMath;

import java.util.Locale;

public final class ParticleItem {
    public static final int SWATCH = 10;

    public final String id;
    public final String key;
    public final String name;
    public final boolean header;
    public final String color;
    public final int px;
    public final String value;
    public final boolean hidden;
    public final boolean changed;

    private ParticleItem(String id, String name, boolean header, ParticleStyle s) {
        this.id = id;
        this.key = id.replace(':', '-').replace('/', '-').replace('.', '-');
        this.name = name;
        this.header = header;
        this.color = TintMath.hex(TintMath.apply(0xFFFFFFFF, s.tint(), s.strength()));
        this.px = Math.max(4, Math.min(16, Math.round(SWATCH * s.sizeFactor())));
        this.hidden = s.hidden();
        this.changed = !s.vanilla();
        this.value = s.hidden() ? Lang.tr(EffectsPart.PREFIX + "ui.hidden") : s.amount() + "%";
    }

    static ParticleItem of(String id, ParticleStyle s) {
        return new ParticleItem(id, ParticleNames.pretty(id), false, s);
    }

    static ParticleItem header(ParticleCategory c) {
        String k = c.name().toLowerCase(Locale.ROOT);
        return new ParticleItem("header-" + k, Lang.tr(EffectsPart.PREFIX + "ui.cat." + k), true, ParticleStyle.VANILLA);
    }
}
