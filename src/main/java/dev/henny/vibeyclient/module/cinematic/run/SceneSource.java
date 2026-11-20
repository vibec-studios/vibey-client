package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.WaypointPoints;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.plan.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class SceneSource {
    private static final long REBUILD_MS = 1000;

    static float partial = 1f;

    private Scene scene;
    private long builtAt;
    private ClientLevel level;
    private McWorld world;

    Scene get(Minecraft mc, double radius, double speed) {
        long now = System.currentTimeMillis();
        if (scene == null || mc.level != level || now - builtAt > REBUILD_MS || scene.radius() != radius || scene.speed() != speed) {
            scene = build(mc, radius, speed);
            builtAt = now;
        }
        return scene;
    }

    void clear() {
        scene = null;
        level = null;
        world = null;
    }

    private Scene build(Minecraft mc, double radius, double speed) {
        LocalPlayer me = mc.player;
        if (level != mc.level) {
            level = mc.level;
            world = new McWorld(mc.level);
        }
        V3 feet = v(me.position());
        V3 eye = v(me.getEyePosition());
        return new Scene(world, eye, feet, me.getYRot(), radius, speed, players(mc, me), marks(mc), Skies.of(mc.level));
    }

    private static List<Scene.Subject> players(Minecraft mc, LocalPlayer me) {
        List<Scene.Subject> out = new ArrayList<>();
        for (AbstractClientPlayer p : mc.level.players()) {
            if (p == me || p.isSpectator() || p.isInvisibleTo(me) || p.isRemoved()) continue;
            out.add(new Scene.Subject(p.getUUID().toString(), p.getName().getString(), () -> v(p.getPosition(partial)), p.getBbHeight()));
        }
        return out;
    }

    private static List<Scene.Mark> marks(Minecraft mc) {
        var wp = ModuleManager.get().get("waypoints");
        if (wp == null || !wp.isEnabled()) return List.of();
        String dim = mc.level.dimension().identifier().toString();
        List<Scene.Mark> out = new ArrayList<>();
        try {
            Services.get(WaypointPoints.class).ifPresent(s -> {
                for (WaypointPoints.Point p : s.points()) {
                    if (dim.equals(p.dimension())) out.add(new Scene.Mark(p.name(), new V3(p.x() + 0.5, p.y(), p.z() + 0.5)));
                }
            });
        } catch (RuntimeException e) {
            return List.of();
        }
        return out;
    }

    static V3 v(Vec3 p) {
        return new V3(p.x, p.y, p.z);
    }
}
