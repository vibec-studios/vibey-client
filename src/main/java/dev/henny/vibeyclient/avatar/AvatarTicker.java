package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.avatar.anim.Clock;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.fx.EmbeddedSounds;
import dev.henny.vibeyclient.avatar.fx.Effects;
import dev.henny.vibeyclient.avatar.fx.SpriteParticles;
import dev.henny.vibeyclient.avatar.render.AvatarDraw;
import dev.henny.vibeyclient.avatar.render.GpuBundles;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Per client tick simulation of every avatar in the world: movement weights for cosmetic animations, the pet's
 * smoothed follow position, emote timing and animation events.
 */
public final class AvatarTicker {
    private AvatarTicker() {}

    private static long tick;

    private static long lastNanos;

    /** Runs at most once per client tick (both the cosmetics and the emotes module call it). */
    public static void tick() {
        long t = System.nanoTime();
        if (t - lastNanos < 25_000_000L) return;
        lastNanos = t;
        tick++;
        GpuBundles.tick();
        SpriteParticles.tick();
        EmbeddedSounds.cleanup();
        double now = Clock.now();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (Player p : mc.level.players()) {
                AvatarState st = AvatarStates.peek(p.getUUID());
                if (st == null) continue;
                st.lastSeenTick = tick;
                move(st, p);
                pet(st, p);
                AvatarStates.tickEmote(st, now);
                Effects.tick(st, now);
            }
            if (tick % 200 == 0) AvatarStates.prune(tick, 600);
        } else {
            for (AvatarState st : AvatarStates.all()) AvatarStates.tickEmote(st, now);
        }
    }

    private static void move(AvatarState st, Player p) {
        float speed = Math.min(1f, p.walkAnimation.speed() * 1.2f);
        st.walkTarget = p.isFallFlying() ? 0 : speed;
        st.flyTarget = p.isFallFlying() || p.getAbilities().flying ? 1 : 0;
        st.sneakTarget = p.isCrouching() ? 1 : 0;
        st.walk = approach(st.walk, st.walkTarget, 0.25f);
        st.fly = approach(st.fly, st.flyTarget, 0.2f);
        st.sneak = approach(st.sneak, st.sneakTarget, 0.3f);
        st.bodyYaw = p.yBodyRot;
        st.x = p.getX();
        st.y = p.getY();
        st.z = p.getZ();
    }

    private static float approach(float v, float target, float k) {
        return v + (target - v) * k;
    }

    /**
     * Pets: "ground" walks next to the player (to the right, slightly behind) with lag, "float" hovers by the
     * shoulder, shoulder pets are drawn on the body (no world position needed).
     */
    private static void pet(AvatarState st, Player p) {
        Bundle b = AvatarDraw.bundle(st, Slot.PET);
        st.petPrevX = st.petX;
        st.petPrevY = st.petY;
        st.petPrevZ = st.petZ;
        st.petPrevYaw = st.petYaw;
        if (b == null) {
            st.petInit = false;
            return;
        }
        Bundle.PetFollow follow = b.pet == null ? Bundle.PetFollow.GROUND : b.pet.follow();
        boolean ground = follow == Bundle.PetFollow.GROUND;
        double a = Math.toRadians(p.yBodyRot);
        // right of the player (+X in the player's frame) and a bit behind
        double side = ground ? 0.95 : 0.7, back = ground ? 0.35 : 0.25;
        double rx = -Math.cos(a), rz = -Math.sin(a); // player's right in world space
        double fx = -Math.sin(a), fz = Math.cos(a);   // forward
        double tx = p.getX() + rx * side - fx * back;
        double tz = p.getZ() + rz * side - fz * back;
        double ty = p.getY() + (ground ? 0 : 1.55 + Math.sin(tick * 0.08) * 0.06);
        if (!st.petInit || st.petX == 0 && st.petZ == 0 || dist2(st.petX, st.petZ, tx, tz) > 12 * 12) {
            st.petX = st.petPrevX = tx;
            st.petY = st.petPrevY = ty;
            st.petZ = st.petPrevZ = tz;
            st.petYaw = st.petPrevYaw = p.yBodyRot;
            st.petInit = true;
            st.petMove = 0;
            return;
        }
        double k = ground ? 0.18 : 0.25;
        double dx = (tx - st.petX) * k, dz = (tz - st.petZ) * k;
        st.petX += dx;
        st.petZ += dz;
        st.petY += (ty - st.petY) * (ground ? 0.35 : 0.2);
        double hs = Math.sqrt(dx * dx + dz * dz);
        st.petMove = approach(st.petMove, (float) Math.min(1.0, hs * 9.0), 0.3f);
        float targetYaw = hs > 0.01 ? (float) Math.toDegrees(Math.atan2(-dx, dz)) : p.yBodyRot;
        float d = ((targetYaw - st.petYaw) % 360f + 540f) % 360f - 180f;
        st.petYaw += d * (hs > 0.01 ? 0.35f : 0.08f);
    }

    private static double dist2(double ax, double az, double bx, double bz) {
        return (ax - bx) * (ax - bx) + (az - bz) * (az - bz);
    }
}
