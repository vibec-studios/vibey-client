package dev.henny.vibeyclient.avatar.fx;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.avatar.AvatarConfig;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.anim.EmotePlay;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.render.AvatarDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runs animation events (particles, sounds) of emotes and cosmetic {@code idle} animations once per client tick:
 * every event whose time was crossed since the last tick fires (loops wrap). Positions come from the locators the
 * last render wrote; before the first render the avatar's position is used.
 */
public final class Effects {
    private Effects() {}

    /** Emote volume (emotes module setting), 0..1. */
    public static volatile float volume = 1f;

    private static final Map<String, ParticleOptions> PARTICLES = new HashMap<>();
    private static final Set<String> BAD_PARTICLES = new HashSet<>();
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    /** Per tick for every avatar in the world (not previews). */
    public static void tick(AvatarState st, double now) {
        EmotePlay e = st.emote;
        if (e != null && AvatarConfig.emotes && !AvatarConfig.emotesServerOff && !e.stopping()) {
            float t = e.localTime;
            run(st, e.bundle, e.anim, e.eventTime, t, e.anim.loop);
            e.eventTime = t;
        }
        if (st.hidden) return;
        for (Slot slot : Slot.all()) {
            int si = slot.ordinal();
            Bundle b = AvatarDraw.bundle(st, slot);
            if (b == null) {
                st.idleEventTime[si] = -1f;
                for (int k = 0; k < AvatarState.MOVE_ANIMS; k++) st.moveEventTime[si * AvatarState.MOVE_ANIMS + k] = -1f;
                continue;
            }
            // idle events always run; the other states' events while that animation is the main one (weight >= 0.5)
            Bundle.Animation idle = b.animation("idle");
            if (idle != null && idle.events.length > 0) {
                float t = AvatarDraw.time(idle, now);
                float last = st.idleEventTime[si];
                if (last >= 0f) run(st, b, idle, last, t, idle.loop);
                st.idleEventTime[si] = t;
            }
            AvatarDraw.weights(st, b, slot, W);
            for (int k = 0; k < AvatarState.MOVE_ANIMS; k++) {
                Bundle.Animation a = b.animation(AvatarDraw.COSMETIC_ANIMS[k + 1]);
                int i = si * AvatarState.MOVE_ANIMS + k;
                if (a == null || a.events.length == 0 || W[k + 1] < 0.5f) {
                    st.moveEventTime[i] = -1f;
                    continue;
                }
                float t = AvatarDraw.time(st, k + 1, a, now);
                float last = st.moveEventTime[i];
                if (last >= 0f) run(st, b, a, last, t, a.loop);
                st.moveEventTime[i] = t;
            }
        }
    }

    private static final float[] W = new float[AvatarDraw.COSMETIC_ANIMS.length];

    /** Fires events with time in (from, to], wrapping for loops. {@code from < 0} = start (includes t = 0). */
    private static void run(AvatarState st, Bundle b, Bundle.Animation a, float from, float to, boolean loop) {
        if (from < 0f) {
            fire(st, b, a, -1f, to);
        } else if (to >= from) {
            fire(st, b, a, from, to);
        } else if (loop) {
            fire(st, b, a, from, a.length);
            fire(st, b, a, -1f, to);
        }
    }

    private static void fire(AvatarState st, Bundle b, Bundle.Animation a, float from, float to) {
        for (Bundle.Event ev : a.events) {
            if (ev.t <= from || ev.t > to + 1e-5f) continue;
            try {
                switch (ev.type) {
                    case PARTICLE -> particle(st, b, ev);
                    case SOUND -> sound(st, b, ev);
                    default -> {}
                }
            } catch (RuntimeException ex) {
                Vibey.LOG.debug("Event of {} failed: {}", b.id, ex.toString());
            }
        }
    }

    // ============================================================================================ particles

    private static final double[] POS = new double[3];

    private static void position(AvatarState st, Bundle.Locator l, double[] out) {
        if (l != null) {
            double[] p = st.locators.get(l.name());
            if (p != null && !Double.isNaN(p[0])) {
                out[0] = p[0];
                out[1] = p[1];
                out[2] = p[2];
                return;
            }
        }
        out[0] = st.x;
        out[1] = st.y + 1.2;
        out[2] = st.z;
    }

    /** Blockbench-space direction (relative to the body) → world, writes into out. */
    private static void toWorld(AvatarState st, float x, float y, float z, float[] out) {
        double a = Math.toRadians(180f - st.bodyYaw);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        out[0] = x * c + z * s;
        out[1] = y;
        out[2] = -x * s + z * c;
    }

    private static final float[] VEL = new float[3], SPREAD = new float[3];

    private static void particle(AvatarState st, Bundle b, Bundle.Event ev) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        position(st, ev.locator, POS);
        toWorld(st, ev.velocity[0], ev.velocity[1], ev.velocity[2], VEL);
        toWorld(st, ev.spread[0], ev.spread[1], ev.spread[2], SPREAD);
        float sx = Math.abs(SPREAD[0]), sy = Math.abs(SPREAD[1]), sz = Math.abs(SPREAD[2]);
        if (ev.sprite >= 0) {
            for (int i = 0; i < ev.count; i++) {
                SpriteParticles.spawn(st, b, ev.sprite, POS[0], POS[1], POS[2], VEL[0], VEL[1], VEL[2], sx, sy, sz,
                        ev.lifetime, ev.size, ev.gravity, ev.emissive);
            }
            return;
        }
        ParticleOptions opt = particleType(ev.id);
        if (opt == null) return;
        for (int i = 0; i < ev.count; i++) {
            double x = POS[0] + (RANDOM.nextFloat() * 2 - 1) * sx;
            double y = POS[1] + (RANDOM.nextFloat() * 2 - 1) * sy;
            double z = POS[2] + (RANDOM.nextFloat() * 2 - 1) * sz;
            mc.level.addParticle(opt, x, y, z, VEL[0], VEL[1], VEL[2]);
        }
    }

    private static ParticleOptions particleType(String id) {
        ParticleOptions o = PARTICLES.get(id);
        if (o != null || BAD_PARTICLES.contains(id)) return o;
        int c = id.indexOf(':');
        var type = BuiltInRegistries.PARTICLE_TYPE.getOptional(Compat.id(id.substring(0, c), id.substring(c + 1))).orElse(null);
        if (type instanceof ParticleOptions po) {
            PARTICLES.put(id, po);
            return po;
        }
        BAD_PARTICLES.add(id);
        Vibey.LOG.info("Particle {} is unknown or needs options; skipped", id);
        return null;
    }

    // ============================================================================================ sounds

    private static void sound(AvatarState st, Bundle b, Bundle.Event ev) {
        Minecraft mc = Minecraft.getInstance();
        position(st, ev.locator, POS);
        float vol = ev.volume * volume;
        if (vol <= 0f) return;
        if (ev.sound >= 0) {
            float game = mc.options.getSoundSourceVolume(SoundSource.MASTER) * mc.options.getSoundSourceVolume(SoundSource.PLAYERS);
            EmbeddedSounds.play(b.sounds[ev.sound], POS[0], POS[1], POS[2], vol * game, ev.pitch);
            return;
        }
        SoundEvent se = SOUNDS.computeIfAbsent(ev.id, id -> {
            int c = id.indexOf(':');
            return SoundEvent.createVariableRangeEvent(Compat.id(id.substring(0, c), id.substring(c + 1)));
        });
        mc.getSoundManager().play(new SimpleSoundInstance(se, SoundSource.PLAYERS, vol, ev.pitch, RANDOM, POS[0], POS[1], POS[2]));
    }
}
