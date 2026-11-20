package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.interact.SizeLimits;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;

final class BeaconFactory {
    static final float BASE = 0.6f;
    private static final SizeLimits LIMITS = new SizeLimits(0.15f, 16f, 0.15f, 16f);

    private BeaconFactory() {}

    static WorldObject create(Waypoint w, boolean foreign, Pos shown, BeaconState state, String dimension) {
        BeaconEffect e = new BeaconEffect(w, foreign, shown, state);
        InteractionRules rules = new InteractionRules().keepAspect(true).edgeResizable(false).limits(LIMITS);
        WorldObject o = new WorldObject("wp:" + w.id() + (foreign ? ":x" : ""), BeaconLayer.KIND)
                .content(e).rules(rules).dimension(dimension).anchoredTo(e.anchor()).locked(foreign).fullBright(true);
        o.transform().size(BASE, BASE).scale(w.size());
        o.visibility().maxDistance(4096).fadeDistance(0);
        e.attach(o);
        return o;
    }
}
