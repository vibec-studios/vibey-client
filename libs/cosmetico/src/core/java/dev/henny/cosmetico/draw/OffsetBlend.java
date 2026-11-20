package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.OffsetRule;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.OffsetWeights;

public final class OffsetBlend {
    private final ShownBundle shown;

    public OffsetBlend(ShownBundle shown) {
        this.shown = shown;
    }

    public void apply(Avatar a, Bundle b, Slot slot, double now, Mix mix) {
        if (b.offsetWhen.isEmpty()) return;
        OffsetWeights ow = a.offsets;
        int n = Slot.count();
        double last = ow.clock[slot.ordinal()];
        double dt = Double.isNaN(last) ? 1.0 : Math.max(0.0, Math.min(1.0, now - last));
        float k = (float) (1.0 - Math.exp(-dt * 12.0));
        for (OffsetRule r : b.offsetWhen) {
            if (r.slot() == slot) continue;
            Bundle other = shown.bundle(a, r.slot());
            boolean worn = other != null && !shown.hiddenByArmor(a, other);
            int i = slot.ordinal() * n + r.slot().ordinal();
            float w = ease(ow.weight[i], worn, k);
            ow.weight[i] = w;
            if (w > 0f) add(mix, r, w * w * (3f - 2f * w));
        }
        ow.clock[slot.ordinal()] = now;
    }

    private static float ease(float w, boolean worn, float k) {
        float target = worn ? 1f : 0f;
        w += (target - w) * k;
        return Math.abs(w - target) < 0.002f ? target : w;
    }

    private static void add(Mix mix, OffsetRule r, float e) {
        for (int c = 0; c < 3; c++) {
            mix.rootOffset[c] += r.offset()[c] * e;
            mix.rootRotation[c] += r.rotation()[c] * e;
        }
        mix.hasRootOffset = true;
    }
}
