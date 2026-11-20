package dev.henny.vibeyclient.module.camera.look;

import dev.henny.vibeyclient.module.camera.CameraModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class CameraState {
    private CameraState() {}

    public static void frame(Minecraft mc) {
        CameraModule m = CameraModule.get();
        FreeLook.update(m, mc);
        Orbit.update(m, mc);
    }

    public static boolean turn(Minecraft mc, double dx, double dy) {
        if (FreeLook.active()) {
            FreeLook.turn(dx, dy);
            return true;
        }
        if (Orbit.capturing() && mc.player != null) {
            Orbit.drag(dx, dy, mc.player.getXRot());
            return true;
        }
        return false;
    }

    public static float yaw(Entity entity, float original) {
        if (!own(entity)) return original;
        if (FreeLook.active()) return FreeLook.yaw();
        Minecraft mc = Minecraft.getInstance();
        return Orbit.applies(mc) ? Orbit.yaw(original) : original;
    }

    public static float pitch(Entity entity, float original) {
        if (!own(entity)) return original;
        if (FreeLook.active()) return FreeLook.pitch();
        Minecraft mc = Minecraft.getInstance();
        return Orbit.applies(mc) ? Orbit.pitch(original) : original;
    }

    public static float distance(Entity entity, float original) {
        if (!own(entity)) return original;
        float z = Orbit.zoom();
        return z == 1f ? original : original * z;
    }

    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        FreeLook.reset(mc);
        Orbit.reset();
    }

    private static boolean own(Entity entity) {
        return entity != null && entity == Minecraft.getInstance().player;
    }
}
