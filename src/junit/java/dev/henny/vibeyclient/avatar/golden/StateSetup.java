package dev.henny.vibeyclient.avatar.golden;

import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.bundle.Bundle;

import java.util.UUID;

final class StateSetup {
    private StateSetup() {}

    static AvatarState avatar(Bundle b) {
        AvatarState st = new AvatarState(UUID.fromString("00000000-0000-0000-0000-000000000001"), false);
        st.slots[b.slot.ordinal()] = b.id;
        st.walk = 0.6f;
        st.fly = 0.3f;
        st.sneak = 0.2f;
        st.sprint = 0.5f;
        st.glide = 0.1f;
        st.happy = 0.4f;
        st.idleLong = 0.3f;
        st.petMove = 0.7f;
        for (int k = 4; k < 8; k++) st.stateStart[k] = PoseDump.NOW - 0.4 * (k - 3);
        return st;
    }
}
