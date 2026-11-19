package dev.henny.cosmetico.sim;

import dev.henny.cosmetico.anim.StateWeights;
import dev.henny.cosmetico.state.Motion;

public final class StateStarts {
    private StateStarts() {}

    public static void update(Motion m, double now) {
        float[] v = {m.sprint, m.glide, m.idleLong, m.happy};
        float[] t = {m.sprintTarget, m.glideTarget, m.idleLongTarget, m.happyTarget};
        for (int i = 0; i < v.length; i++) {
            int k = StateWeights.SPRINT + i;
            if (t[i] > 0 && Double.isNaN(m.stateStart[k])) m.stateStart[k] = now;
            else if (t[i] <= 0 && v[i] < 0.01f) m.stateStart[k] = Double.NaN;
        }
    }
}
