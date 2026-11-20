package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.plan.Session;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

public final class CameraDriver {
    public interface Target {
        void vibey$place(double x, double y, double z, float yaw, float pitch, boolean detached);
    }

    private static float vanillaFov = 70;
    private static float fov = -1;
    private static boolean away;

    private CameraDriver() {}

    public static void afterSetup(Camera camera, float partial, Target target) {
        if (!Cinematic.active()) {
            away = false;
            fov = -1;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Pose player = new Pose(SceneSource.v(camera.position()), camera.yRot(), camera.xRot(), vanillaFov);
        Session.Frame f = Cinematic.update(mc, player, partial);
        if (f == null) return;
        Pose p = f.pose();
        V3 pos = p.pos();
        away = pos.dist(player.pos()) > 1.0;
        fov = p.fov();
        target.vibey$place(pos.x(), pos.y(), pos.z(), p.yaw(), p.pitch(), away || camera.isDetached());
    }

    public static float fov(float original, boolean useSetting) {
        if (!useSetting) return original;
        vanillaFov = original;
        return Cinematic.active() && fov > 0 ? fov : original;
    }

    public static boolean hideHand() {
        if (!Cinematic.active()) return false;
        Session.Frame f = Cinematic.frame();
        return away || f != null && f.fade() > 0.5;
    }
}
