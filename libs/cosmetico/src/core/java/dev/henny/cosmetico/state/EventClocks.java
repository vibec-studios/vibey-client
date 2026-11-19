package dev.henny.cosmetico.state;

import dev.henny.cosmetico.model.Slot;

import java.util.Arrays;

public final class EventClocks {
    public static final int MOVE_ANIMS = 7;
    public final float[] idle = new float[Slot.count()];
    public final float[] move = new float[Slot.count() * MOVE_ANIMS];

    public EventClocks() {
        Arrays.fill(idle, -1f);
        Arrays.fill(move, -1f);
    }

    public void reset(Slot slot) {
        int si = slot.ordinal();
        idle[si] = -1f;
        for (int k = 0; k < MOVE_ANIMS; k++) move[si * MOVE_ANIMS + k] = -1f;
    }
}
