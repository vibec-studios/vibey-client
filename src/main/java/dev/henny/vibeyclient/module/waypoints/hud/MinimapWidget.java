package dev.henny.vibeyclient.module.waypoints.hud;

import dev.henny.vibeyclient.module.waypoints.WaypointsModule;
import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.map.MinimapMath;
import dev.henny.vibeyclient.module.waypoints.map.MapPainter;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudPaint;
import dev.henny.vibeyclient.ui.hud.HudWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class MinimapWidget extends HudWidget {
    static final int SIZE = 96;
    static final double[] ZOOMS = {0.5, 1, 2, 4};
    private static final int BG = 0xE00C0C12, LINE = 0xFF2A2A38;

    private final MinimapDots dots = new MinimapDots();
    private final float[] pt = new float[2];
    static volatile int lastTiles;
    static volatile long lastPaintNanos;

    @Override
    protected void update(HudPaint p) {
        width = SIZE;
        height = SIZE;
        dots.refresh(WaypointsRuntime.get());
    }

    @Override
    protected void paint(HudPaint p) {
        long start = System.nanoTime();
        Gfx g = p.g;
        int w = p.w, h = p.h;
        g.rect(0, 0, w, h, BG);
        WaypointsRuntime rt = WaypointsRuntime.get();
        Player pl = Minecraft.getInstance().player;
        if (rt == null || pl == null || !rt.active() || rt.map().dimension() == null) {
            MinimapMarks.arrow(g, w / 2f, h / 2f, 0f);
            g.outline(0, 0, w, h, LINE);
            return;
        }
        WaypointsModule m = rt.module();
        float yaw = pl.getYRot();
        float rot = MinimapMath.rotation(yaw, m.minimapRotate.get());
        double zoom = ZOOMS[Math.max(0, Math.min(ZOOMS.length - 1, m.minimapZoom.get() - 1))];
        double half = Math.min(w, h) / 2.0;
        g.scissor(1, 1, w - 1, h - 1);
        g.push();
        g.translate(w / 2f, h / 2f);
        g.rotate(rot);
        lastTiles = MapPainter.draw(g, rt.map().textures(), rt.map().dimension(), pl.getX(), pl.getZ(), zoom, half * 1.42, half * 1.42);
        g.pop();
        dots.paint(g, pl.getX(), pl.getZ(), rot, zoom, w, h, pt);
        MinimapMarks.north(g, rot, w, h, pt);
        MinimapMarks.arrow(g, w / 2f, h / 2f, m.minimapRotate.get() ? 0f : (float) Math.toRadians(yaw + 180f));
        g.endScissor();
        g.outline(0, 0, w, h, LINE);
        lastPaintNanos = System.nanoTime() - start;
    }
}
