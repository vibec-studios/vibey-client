package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vibeyclient.module.waypoints.core.Colors;
import dev.henny.vibeyclient.module.waypoints.core.Navigation;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointKind;
import dev.henny.vibeyclient.module.waypoints.view.CoordText;
import dev.henny.vibeyclient.module.waypoints.view.Placement;
import net.minecraft.world.entity.player.Player;

public final class WaypointRow {
    public final String id;
    public final String name;
    public final String color;
    public final String dist;
    public final String coords;
    public final String portal;
    public final String dimName;
    public final boolean visible;
    public final boolean nav;
    public final boolean death;

    WaypointRow(Waypoint w, Player p, String dimension, String navId) {
        id = w.id();
        name = w.name();
        color = "#" + Colors.hex(w.color());
        Pos shown = Placement.shownPos(w, dimension);
        dist = shown == null || p == null ? "" : Navigation.distanceText(Navigation.distance(p.getX(), p.getY(), p.getZ(), shown));
        coords = CoordText.xyz(w.pos());
        portal = CoordText.counterpart(w.pos(), w.dimension());
        dimName = CoordText.dimensionName(w.dimension());
        visible = w.visible();
        nav = w.id().equals(navId);
        death = w.kind() == WaypointKind.DEATH;
    }
}
