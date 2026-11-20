package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.waypoints.WaypointsModule;
import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointList;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.view.Placement;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BeaconLayer {
    public static final String KIND = "vibeyclient:waypoint";

    private final WaypointsRuntime runtime;
    private final BeaconState state = new BeaconState();
    private final Map<String, WorldObject> objects = new LinkedHashMap<>();
    private boolean attached;
    private int revision = -1;
    private String dimension;
    private boolean other;

    public BeaconLayer(WaypointsRuntime runtime) {
        this.runtime = runtime;
    }

    public void attach() {
        if (attached) return;
        attached = true;
        R3Mc.scene().interaction().listen(new BeaconGestures(runtime)::onEvent);
    }

    public WorldObject object(String waypointId) {
        return objects.get(waypointId);
    }

    public void sync(Minecraft mc) {
        if (!attached) return;
        WaypointsModule m = runtime.module();
        if (!m.beacons.get()) {
            clear();
            return;
        }
        state.update(runtime.navigator().targetId(), m.beams.get());
        String d = WorldContext.dimension(mc.level);
        WaypointList list = runtime.store().list();
        boolean o = m.otherDimension.get();
        if (list.revision() != revision || !d.equals(dimension) || o != other || missing()) rebuild(list, d, o);
        for (WorldObject obj : objects.values()) {
            BeaconEffect e = (BeaconEffect) obj.content();
            Pos p = Placement.shownPos(e.waypoint(), d);
            e.shownPos(p);
            obj.visibility().hidden(p != null && BeaconAnchor.standingAt(mc.player, p));
        }
    }

    private void rebuild(WaypointList list, String d, boolean showOther) {
        clear();
        revision = list.revision();
        dimension = d;
        other = showOther;
        for (Waypoint w : list.all()) {
            if (!w.visible()) continue;
            boolean foreign = Placement.foreign(w, d);
            if (foreign && !showOther) continue;
            Pos p = Placement.shownPos(w, d);
            if (p == null) continue;
            WorldObject o = BeaconFactory.create(w, foreign, p, state, d);
            objects.put(w.id(), o);
            R3Mc.add(o);
        }
    }

    private boolean missing() {
        for (WorldObject o : objects.values()) if (R3Mc.scene().get(o.id()) != o) return true;
        return false;
    }

    public void clear() {
        if (attached) for (WorldObject o : objects.values()) R3Mc.remove(o);
        objects.clear();
        revision = -1;
        dimension = null;
    }
}
