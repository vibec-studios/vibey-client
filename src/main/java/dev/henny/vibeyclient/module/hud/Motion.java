package dev.henny.vibeyclient.module.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** The player's speed in blocks per second (horizontal and 3D), from the position change per tick, smoothed. */
final class Motion {
    private Motion() {}

    private static double lastX, lastY, lastZ;
    private static boolean has;
    private static double horizontal, full;

    static void tick() {
        Player p = Minecraft.getInstance().player;
        if (p == null) {
            reset();
            return;
        }
        double x = p.getX(), y = p.getY(), z = p.getZ();
        if (has) {
            double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
            double h = Math.sqrt(dx * dx + dz * dz) * 20.0;
            double f = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
            // teleports are not speed
            if (f > 200) h = f = 0;
            horizontal += (h - horizontal) * 0.35;
            full += (f - full) * 0.35;
            if (horizontal < 0.005) horizontal = 0;
            if (full < 0.005) full = 0;
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        has = true;
    }

    static void reset() {
        has = false;
        horizontal = full = 0;
    }

    static double horizontal() {
        return horizontal;
    }

    static double full() {
        return full;
    }
}
