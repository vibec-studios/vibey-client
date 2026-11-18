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
                move(st, p, now);
                pet(st, p);
                AvatarStates.tickEmote(st, now);
                Effects.tick(st, now);
            }
            if (tick % 200 == 0) AvatarStates.prune(tick, 600);
        } else {
            for (AvatarState st : AvatarStates.all()) AvatarStates.tickEmote(st, now);
        }
    }

    /** Standing still this long (ticks) starts {@code idle_long}; falling this long starts {@code glide}. */
    static final int IDLE_LONG_TICKS = 200, FALL_GLIDE_TICKS = 14;

    private static void move(AvatarState st, Player p, double now) {
        float speed = Math.min(1f, p.walkAnimation.speed() * 1.2f);
        boolean flying = p.getAbilities().flying;
        boolean elytra = p.isFallFlying();
        double dx = p.getX() - st.x, dy = p.getY() - st.y, dz = p.getZ() - st.z;
        if (!st.moveInit) {
            dx = dy = dz = 0;
            st.moveInit = true;
        }
        // falling for a while (not elytra, not creative flight, not in water) counts as gliding
        boolean falling = dy < -0.2 && !p.onGround() && !flying && !elytra && !p.isInWater();
        st.fallTicks = falling ? st.fallTicks + 1 : 0;
        boolean still = Math.abs(dx) + Math.abs(dz) < 0.004 && Math.abs(dy) < 0.02 && speed < 0.05 && !flying && !elytra
                && p.onGround() && st.emote == null;
        st.stillTicks = still ? st.stillTicks + 1 : 0;

        st.walkTarget = elytra ? 0 : speed;
        st.flyTarget = elytra || flying ? 1 : 0;
        st.sneakTarget = p.isCrouching() ? 1 : 0;
        // sprinting = the flag and actual movement (walk animation, or the position change for players whose walk
        // animation is not updated, e.g. entities moved by the game/tests)
        boolean moving = speed > 0.3f || dx * dx + dz * dz > 0.08 * 0.08;
        st.sprintTarget = p.isSprinting() && !p.isSwimming() && !elytra && !flying && moving ? 1 : 0;
        st.glideTarget = elytra || st.fallTicks > FALL_GLIDE_TICKS ? 1 : 0;
        st.idleLongTarget = st.stillTicks > IDLE_LONG_TICKS ? 1 : 0;
        st.happyTarget = st.emote != null && !st.emote.stopping() ? 1 : 0;
        st.walk = approach(st.walk, st.walkTarget, 0.25f);
        st.fly = approach(st.fly, st.flyTarget, 0.2f);
        st.sneak = approach(st.sneak, st.sneakTarget, 0.3f);
        st.sprint = approach(st.sprint, st.sprintTarget, 0.2f);
        st.glide = approach(st.glide, st.glideTarget, 0.15f);
        // settle slowly into idle_long (~1.5 s), wake up quickly
        st.idleLong = approach(st.idleLong, st.idleLongTarget, st.idleLongTarget > st.idleLong ? 0.07f : 0.35f);
        st.happy = approach(st.happy, st.happyTarget, 0.25f);
        states(st, now);
        st.bodyYaw = p.yBodyRot;
        st.x = p.getX();
        st.y = p.getY();
        st.z = p.getZ();
    }

    /** Start times of the optional states: set when a state begins, cleared when it has faded out. */
    public static void states(AvatarState st, double now) {
        float[] v = {st.sprint, st.glide, st.idleLong, st.happy};
        float[] t = {st.sprintTarget, st.glideTarget, st.idleLongTarget, st.happyTarget};
        for (int i = 0; i < v.length; i++) {
            int k = AvatarDraw.SPRINT + i;
            if (t[i] > 0 && Double.isNaN(st.stateStart[k])) st.stateStart[k] = now;
            else if (t[i] <= 0 && v[i] < 0.01f) st.stateStart[k] = Double.NaN;
        }
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
        // right of the player (+X in the player's frame) and a bit behind; big pets keep more distance
        double half = petHalfWidth(b);
        double side = ground ? petSide(b) : 0.7, back = ground ? 0.35 + Math.max(0, half - 0.3) * 0.5 : 0.25;
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
        if (ground) {
            // never walk through the player: keep the pet's footprint outside the player's (0.3 blocks radius)
            double ox = st.petX - p.getX(), oz = st.petZ - p.getZ();
            double d = Math.sqrt(ox * ox + oz * oz), min = 0.3 + half + 0.08;
            if (d < min) {
                if (d < 1e-4) {
                    ox = rx;
                    oz = rz;
                    d = 1;
                }
                double push = Math.min(min - d, 0.25);
                st.petX += ox / d * push;
                st.petZ += oz / d * push;
                dx += ox / d * push;
                dz += oz / d * push;
            }
        }
        st.petY += (ty - st.petY) * (ground ? 0.35 : 0.2);
        double hs = Math.sqrt(dx * dx + dz * dz);
        st.petMove = approach(st.petMove, (float) Math.min(1.0, hs * 9.0), 0.3f);
        float targetYaw = hs > 0.01 ? (float) Math.toDegrees(Math.atan2(-dx, dz)) : p.yBodyRot;
        float d = ((targetYaw - st.petYaw) % 360f + 540f) % 360f - 180f;
        st.petYaw += d * (hs > 0.01 ? 0.35f : 0.08f);
    }

    private static final java.util.Map<Bundle, Double> HALF_WIDTH = new java.util.WeakHashMap<>();

    /**
     * Half the width (X extent) of a pet in blocks at its {@code pet.scale}: the widest body cube of the rest pose
     * (cubes thinner than 2 px are ignored), from the model origin. Big pets keep more distance to the player.
     */
    public static double petHalfWidth(Bundle b) {
        if (b == null || b.model == null) return 0.25;
        return HALF_WIDTH.computeIfAbsent(b, k -> {
            float m = 0;
            for (Bundle.Bone bone : k.model.bones) {
                for (Bundle.Cube c : bone.cubes) {
                    // the body counts, thin parts (flippers, whiskers, planes) may stick out
                    if (Math.abs(c.to[1] - c.from[1]) < 2f || Math.abs(c.to[2] - c.from[2]) < 2f) continue;
                    m = Math.max(m, Math.max(Math.abs(c.from[0]), Math.abs(c.to[0])) + c.inflate);
                }
            }
            float scale = k.pet == null ? 1f : k.pet.scale();
            return Math.min(1.5, Math.max(0.1, m * scale / 16.0));
        });
    }

    /** Sideways follow distance (blocks, to the player's right) of a ground pet. */
    public static double petSide(Bundle b) {
        return Math.max(0.95, 0.3 + petHalfWidth(b) + 0.3);
    }

    private static double dist2(double ax, double az, double bx, double bz) {
        return (ax - bx) * (ax - bx) + (az - bz) * (az - bz);
    }
}
