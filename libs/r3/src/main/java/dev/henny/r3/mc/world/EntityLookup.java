package dev.henny.r3.mc.world;

import dev.henny.r3.anchor.EntityPose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class EntityLookup {
    private EntityLookup() {}

    public static boolean pose(ClientLevel level, UUID id, float partialTick, EntityPose out) {
        Entity e = find(level, id);
        if (e == null || e.isRemoved()) return false;
        Vec3 p = e.getPosition(partialTick);
        float yaw = e instanceof LivingEntity living ? lerpDegrees(living.yBodyRotO, living.yBodyRot, partialTick) : e.getYRot();
        out.set(p.x, p.y, p.z, e.getBbHeight(), yaw, e.isCrouching());
        return true;
    }

    private static Entity find(ClientLevel level, UUID id) {
        Entity player = level.getPlayerByUUID(id);
        if (player != null) return player;
        for (Entity e : level.entitiesForRendering()) {
            if (id.equals(e.getUUID())) return e;
        }
        return null;
    }

    static float lerpDegrees(float from, float to, float t) {
        float d = ((to - from) % 360f + 540f) % 360f - 180f;
        return from + d * t;
    }
}
