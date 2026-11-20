package dev.henny.vibeyclient.module.visuals.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PreviewSpot {
    public static final float LEFT_DEGREES = -22f;

    private PreviewSpot() {}

    public static Vec3 ahead(double distance) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return Vec3.ZERO;
        float yaw = (float) Math.toRadians(p.getYRot() + LEFT_DEGREES);
        float pitch = (float) Math.toRadians(Math.max(-30f, Math.min(30f, p.getXRot())));
        double flat = Math.cos(pitch) * distance;
        Vec3 eye = p.getEyePosition();
        return new Vec3(eye.x - Math.sin(yaw) * flat, eye.y - Math.sin(pitch) * distance - 0.2, eye.z + Math.cos(yaw) * flat);
    }
}
