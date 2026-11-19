package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.Motion;

import java.util.UUID;

public final class StateSetup {
    private StateSetup() {}

    public static Avatar avatar(Bundle b) {
        Avatar a = new Avatar(UUID.fromString("00000000-0000-0000-0000-000000000001"), false);
        a.loadout.base[b.slot.ordinal()] = b.id;
        Motion m = a.motion;
        m.walk = 0.6f;
        m.fly = 0.3f;
        m.sneak = 0.2f;
        m.sprint = 0.5f;
        m.glide = 0.1f;
        m.happy = 0.4f;
        m.idleLong = 0.3f;
        a.pet.move = 0.7f;
        for (int k = 4; k < 8; k++) m.stateStart[k] = PoseDump.NOW - 0.4 * (k - 3);
        return a;
    }
}
