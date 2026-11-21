package dev.henny.vibeyclient.module.serverapi;

import dev.henny.vibeyclient.api.ModuleCategory;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.ServerPresentationClient;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class ServerApiModule extends VibeyModule {
    public ServerApiModule() {
        super("serverapi", ModuleCategory.MULTIPLAYER);
    }

    public static boolean enabled() {
        ServerApiModule module = ModuleManager.get().get(ServerApiModule.class);
        return module != null && module.isEnabled();
    }

    @Override
    public void renderHud(Gfx g) {
        var waypoints = ServerPresentationClient.waypoints();
        if (waypoints.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || ClientCompat.screen() != null || g.screenWidth() < 40) return;
        String dimension = mc.level.dimension().identifier().toString();
        //? if >=26.2 {
        /*var camera = mc.gameRenderer.mainCamera();
        *///?} else
        var camera = mc.gameRenderer.getMainCamera();
        var forward = camera.forwardVector();
        for (var waypoint : waypoints) {
            if (!waypoint.dimension().equals(dimension)) continue;
            Vec3 point = new Vec3(waypoint.x() + 0.5, waypoint.y() + 0.5, waypoint.z() + 0.5);
            Vec3 relative = point.subtract(camera.position());
            if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() < 0.2) continue;
            Vec3 projected = mc.gameRenderer.projectPointToScreen(point);
            if (!Double.isFinite(projected.x) || !Double.isFinite(projected.y)
                    || Math.abs(projected.x) > 0.98 || Math.abs(projected.y) > 0.94) continue;
            int x = (int) ((1 + projected.x) * g.screenWidth() / 2);
            int y = (int) ((1 - projected.y) * g.screenHeight() / 2);
            long distance = Math.round(mc.player.position().distanceTo(point));
            String label = g.clip(tr("marker", waypoint.label(), distance), Math.max(24, g.screenWidth() / 2));
            int width = g.width(label);
            int left = Math.clamp(x - width / 2, 5, g.screenWidth() - width - 5);
            g.rect(left - 4, y - 2, width + 8, g.lineHeight() + 4, 0xE0000000);
            g.rect(left - 4, y - 2, 2, g.lineHeight() + 4, 0xFF000000 | waypoint.color());
            g.text(label, left, y, 0xFFFFFFFF);
        }
    }
}
