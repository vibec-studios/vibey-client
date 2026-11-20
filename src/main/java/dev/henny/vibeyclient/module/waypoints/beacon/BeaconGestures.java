package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.screen.Screens;

final class BeaconGestures {
    private final WaypointsRuntime runtime;

    BeaconGestures(WaypointsRuntime runtime) {
        this.runtime = runtime;
    }

    void onEvent(InteractionEvent e) {
        if (!(e.target() instanceof WorldObject o) || !BeaconLayer.KIND.equals(o.kind())) return;
        if (!(o.content() instanceof BeaconEffect b)) return;
        Waypoint w = b.waypoint();
        switch (e.kind()) {
            case ACTION -> runtime.actions().toggleNavigate(w);
            case CONTEXT_MENU -> Tasks.onMain(() -> Screens.list(w.id()));
            case GRAB_STARTED -> b.moves().grabbed(w.pos(), o.transform().position());
            case MOVED, PLACED -> moved(o, b, w);
            case RESIZED -> resized(o, b, w);
            case UNDONE -> undone(o, b, w);
            default -> {
            }
        }
    }

    private void moved(WorldObject o, BeaconEffect b, Waypoint w) {
        if (b.foreign()) return;
        w.pos(BeaconMoves.target(o));
        b.moves().committed();
        resized(o, b, w);
    }

    private void resized(WorldObject o, BeaconEffect b, Waypoint w) {
        w.size((float) (o.transform().scale() / b.anchor().factor()));
        o.anchor(b.anchor());
        runtime.actions().edited(w);
    }

    private void undone(WorldObject o, BeaconEffect b, Waypoint w) {
        if (!b.foreign()) {
            if (o.anchor() != b.anchor()) w.pos(BeaconMoves.target(o));
            else if (b.moves().undoMatches(o.transform().position())) w.pos(b.moves().pop());
        }
        resized(o, b, w);
    }
}
