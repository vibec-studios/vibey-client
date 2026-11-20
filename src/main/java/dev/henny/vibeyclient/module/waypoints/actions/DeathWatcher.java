package dev.henny.vibeyclient.module.waypoints.actions;

import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import net.minecraft.client.Minecraft;

public final class DeathWatcher {
    private boolean dead;

    public void tick(Minecraft mc, WaypointActions actions) {
        boolean now = mc.player.isDeadOrDying();
        if (now && !dead) actions.markDeath(WorldContext.feet(mc.player), WorldContext.dimension(mc.level));
        dead = now;
    }
}
