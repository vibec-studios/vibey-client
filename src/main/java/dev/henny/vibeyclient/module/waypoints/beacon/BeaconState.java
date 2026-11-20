package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.interact.Interaction;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;

final class BeaconState {
    private String navTargetId;
    private boolean beams = true;

    boolean hovered(WorldObject o) {
        Interaction i = R3Mc.scene().interaction();
        return o != null && (i.hover().target() == o || i.held() == o);
    }

    String navTargetId() {
        return navTargetId;
    }

    boolean beams() {
        return beams;
    }

    void update(String navTargetId, boolean beams) {
        this.navTargetId = navTargetId;
        this.beams = beams;
    }
}
