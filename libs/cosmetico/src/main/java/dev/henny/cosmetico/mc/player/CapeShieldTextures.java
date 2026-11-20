package dev.henny.cosmetico.mc.player;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.draw.ShownBundle;
import dev.henny.cosmetico.mc.render.GpuBundle;
import dev.henny.cosmetico.mc.render.GpuCache;
import dev.henny.cosmetico.mc.render.Tex;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.state.Avatar;

public final class CapeShieldTextures {
    private CapeShieldTextures() {}

    public static Tex cape(Avatar a) {
        if (a == null || a.frame.hidden || !Cosmetico.config().slotVisible(Slot.CAPE)) return null;
        ShownBundle shown = Cosmetico.engine().shown;
        Bundle b = shown.bundle(a, Slot.CAPE);
        if (b == null || b.capeTexture < 0 || shown.hiddenByArmor(a, b)) return null;
        GpuBundle gb = GpuCache.get(b);
        return gb == null ? null : gb.texture(b.capeTexture);
    }

    public static Tex shield(Avatar a) {
        if (a == null || a.frame.hidden || !Cosmetico.config().slotVisible(Slot.SHIELD)) return null;
        Bundle b = Cosmetico.engine().shown.bundle(a, Slot.SHIELD);
        if (b == null || b.shieldTexture < 0) return null;
        GpuBundle gb = GpuCache.get(b);
        return gb == null ? null : gb.texture(b.shieldTexture);
    }
}
