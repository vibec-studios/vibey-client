package dev.henny.companio.mc.world;

import dev.henny.companio.sense.Owner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class OwnerTracker {
    private double lastX, lastY, lastZ;
    private float lastYaw, lastPitch;
    private boolean wasOnGround = true;
    private int idleTicks;

    public void fill(Player p, Owner o, boolean lookingAtCompanion) {
        o.present = true;
        o.x = p.getX();
        o.y = p.getY();
        o.z = p.getZ();
        o.eyeY = p.getEyeY();
        o.yaw = p.getYRot();
        o.bodyYaw = p.yBodyRot;
        o.pitch = p.getXRot();
        double dx = o.x - lastX, dy = o.y - lastY, dz = o.z - lastZ;
        o.speed = Math.sqrt(dx * dx + dz * dz) * 20;
        boolean moved = dx * dx + dy * dy + dz * dz > 1e-4 || Math.abs(o.yaw - lastYaw) > 0.5f || Math.abs(o.pitch - lastPitch) > 0.5f;
        idleTicks = moved ? 0 : idleTicks + 1;
        o.idle = idleTicks / 20.0;
        o.sneaking = p.isShiftKeyDown();
        o.sprinting = p.isSprinting();
        o.swimming = p.isSwimming();
        o.elytra = p.isFallFlying();
        o.flying = p.getAbilities().flying;
        o.sleeping = p.isSleeping();
        o.eating = p.isUsingItem() && eats(p.getUseItem());
        o.onGround = p.onGround();
        o.inWater = p.isInWater();
        o.riding = p.isPassenger();
        o.hurt = p.hurtTime > 0;
        o.health = p.getMaxHealth() <= 0 ? 1f : p.getHealth() / p.getMaxHealth();
        o.held = id(p.getMainHandItem());
        o.offhand = id(p.getOffhandItem());
        o.jumped = wasOnGround && !p.onGround() && dy > 0.2 && !o.flying && !o.inWater;
        o.looking = lookingAtCompanion;
        wasOnGround = p.onGround();
        lastX = o.x;
        lastY = o.y;
        lastZ = o.z;
        lastYaw = o.yaw;
        lastPitch = o.pitch;
    }

    private static boolean eats(ItemStack s) {
        return s.has(DataComponents.FOOD) || s.has(DataComponents.CONSUMABLE);
    }

    private static String id(ItemStack s) {
        return s.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }
}
