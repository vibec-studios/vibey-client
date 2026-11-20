package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.vibeyclient.module.waypoints.core.Navigation;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.view.CoordText;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class BeaconLabels {
    private String title = "";
    private String coords = "";
    private String portal = "";

    String title() {
        return title;
    }

    String coords() {
        return coords;
    }

    String portal() {
        return portal;
    }

    void refresh(BeaconEffect b) {
        Player p = Minecraft.getInstance().player;
        Pos shown = b.shownPos();
        if (p == null || shown == null) return;
        Waypoint w = b.waypoint();
        String distance = Navigation.distanceText(Navigation.distance(p.getX(), p.getY(), p.getZ(), shown));
        title = (b.foreign() ? "⇄ " : "") + w.name() + "  " + distance;
        coords = CoordText.xyz(shown);
        portal = CoordText.counterpart(shown, b.foreign() ? null : w.dimension());
    }
}
