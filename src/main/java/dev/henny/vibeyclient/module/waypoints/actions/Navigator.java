package dev.henny.vibeyclient.module.waypoints.actions;

import dev.henny.vibeyclient.module.waypoints.core.Navigation;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointList;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.view.Placement;
import net.minecraft.client.Minecraft;

public final class Navigator {
    private String targetId;
    private Waypoint target;
    private Pos shown;

    public String targetId() {
        return targetId;
    }

    public Waypoint target() {
        return target;
    }

    public Pos shownPos() {
        return shown;
    }

    public void target(String id) {
        targetId = id;
    }

    public void clear() {
        targetId = null;
        target = null;
        shown = null;
    }

    public void tick(Minecraft mc, WaypointList list) {
        if (targetId == null) return;
        Waypoint w = list.get(targetId);
        Pos p = w == null ? null : Placement.shownPos(w, WorldContext.dimension(mc.level));
        if (p == null) {
            target = null;
            shown = null;
            if (w == null) clear();
            return;
        }
        target = w;
        shown = p;
        if (Navigation.distance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), p) < Navigation.ARRIVED) clear();
    }
}
