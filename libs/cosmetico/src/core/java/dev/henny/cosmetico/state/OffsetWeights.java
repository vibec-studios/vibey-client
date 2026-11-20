package dev.henny.cosmetico.state;

import dev.henny.cosmetico.model.Slot;

import java.util.Arrays;

public final class OffsetWeights {
    public final float[] weight = new float[Slot.count() * Slot.count()];
    public final double[] clock = new double[Slot.count()];

    public OffsetWeights() {
        Arrays.fill(clock, Double.NaN);
    }
}
