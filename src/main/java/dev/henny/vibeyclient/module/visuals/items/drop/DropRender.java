package dev.henny.vibeyclient.module.visuals.items.drop;

import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.module.visuals.items.core.DropScale;

public final class DropRender {
    private DropRender() {}

    public static float dropped() {
        VisualsModule m = VisualsModule.get();
        if (m == null || !m.isEnabled()) return 1f;
        int pct = m.drops.dropScale.get();
        return pct == 100 ? 1f : DropScale.factor(pct);
    }

    public static float framed() {
        VisualsModule m = VisualsModule.get();
        if (m == null || !m.isEnabled()) return 1f;
        int pct = m.drops.frameScale.get();
        return pct == 100 ? 1f : DropScale.factor(pct);
    }
}
