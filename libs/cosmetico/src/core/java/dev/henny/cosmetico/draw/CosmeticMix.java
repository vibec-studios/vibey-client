package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.anim.AnimTime;
import dev.henny.cosmetico.anim.StateWeights;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.state.Avatar;

public final class CosmeticMix {
    private final Mix mix = new Mix();
    private final float[] w = new float[StateWeights.COUNT];
    private final StateWeightsInput weights = new StateWeightsInput();
    private final OffsetBlend offsets;

    public CosmeticMix(ShownBundle shown) {
        this.offsets = new OffsetBlend(shown);
    }

    public Mix of(Avatar a, Bundle b, Slot slot, double now) {
        weights.weights(a, b, slot, w);
        mix.clear();
        float best = -1f;
        for (int k = 0; k < StateWeights.COUNT; k++) {
            Animation anim = b.animation(StateWeights.NAMES[k]);
            if (anim == null) continue;
            float t = AnimTime.state(a.motion.stateStart, k, anim, now);
            mix.add(anim, t, w[k]);
            float score = k == StateWeights.IDLE ? Math.max(w[0], 0.5f) : w[k];
            if (anim.hasVisibility && score > best) {
                best = score;
                mix.visibility = anim;
                mix.visibilityTime = t;
            }
        }
        if (mix.visibility == null) idleVisibility(b, now);
        offsets.apply(a, b, slot, now, mix);
        return mix;
    }

    public float[] lastWeights() {
        return w;
    }

    public StateWeightsInput weights() {
        return weights;
    }

    private void idleVisibility(Bundle b, double now) {
        Animation idle = b.animation("idle");
        if (idle == null) return;
        mix.visibility = idle;
        mix.visibilityTime = AnimTime.global(idle, now);
    }
}
