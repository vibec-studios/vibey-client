package dev.henny.vibeyclient.module.waypoints.hud;

import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Navigation;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class NavArrow {
    static final int RADIUS = 22;

    private static String text = "";
    private static long nextText;

    private NavArrow() {}

    public static void draw(Gfx g, WaypointsRuntime rt) {
        Waypoint w = rt.navigator().target();
        Pos p = rt.navigator().shownPos();
        Player pl = Minecraft.getInstance().player;
        if (w == null || p == null || pl == null || Minecraft.getInstance().screen != null) return;
        double dx = p.x() + 0.5 - pl.getX(), dz = p.z() + 0.5 - pl.getZ();
        float rel = Navigation.relativeYaw(pl.getYRot(), dx, dz);
        float a = (float) Math.toRadians(rel);
        float cx = g.screenWidth() / 2f, cy = g.screenHeight() / 2f;
        int color = 0xFF000000 | w.color();
        g.push();
        g.translate(cx + (float) Math.sin(a) * RADIUS, cy - (float) Math.cos(a) * RADIUS);
        g.rotate(a);
        int aw = g.width(MinimapMarks.ARROW);
        g.textShadow(MinimapMarks.ARROW, -aw / 2, -4, color);
        g.pop();
        long now = System.currentTimeMillis();
        if (now >= nextText) {
            nextText = now + 250;
            text = w.name() + "  " + Navigation.distanceText(Navigation.distance(pl.getX(), pl.getY(), pl.getZ(), p));
        }
        g.textShadow(text, Math.round(cx) - g.width(text) / 2, Math.round(cy) + RADIUS + 8, color);
    }
}
