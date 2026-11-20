package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.anim.StateWeights;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.Motion;

public final class StateWeightsInput {
    private final boolean[] has = new boolean[StateWeights.COUNT];

    public void weights(Avatar a, Bundle b, Slot slot, float[] out) {
        for (int k = 0; k < has.length; k++) has[k] = present(b.animation(StateWeights.NAMES[k]), k);
        Motion m = a.motion;
        float move = slot == Slot.PET ? a.pet.move : m.walk;
        StateWeights.compute(has, m.glide, m.fly, move, m.sprint, m.sneak, m.happy, m.idleLong, out);
    }

    public static boolean present(Animation a, int k) {
        return a != null && (k <= StateWeights.SNEAK || a.tracks.length > 0 || a.events.length > 0);
    }
}
