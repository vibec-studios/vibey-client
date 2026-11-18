package dev.henny.vibeyclient.module.media.lyrics.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.render.LyricLayout;
import dev.henny.vibeyclient.module.media.lyrics.render.WorldSink;
import dev.henny.vibeyclient.module.media.lyrics.sky.SkyTheme;
import org.joml.Matrix4f;

/**
 * The sky's music effects, drawn on top of the custom sky (and, at night, the vanilla sky): the sky's color drifts
 * with the energy, the horizon glows with the bass and swells on beats, beats make a group of stars twinkle (shimmer
 * motes on day skies), aurora curtains brighten with the mids and highs, a drop blooms the horizon (and, only with
 * the setting on, gives one soft flash). Tasteful and epilepsy-safe by default: every effect eases in and out, beats
 * light up at most 3 times per second, a flash at most every 2.5 s and never brighter than a third, and *Effect
 * strength* scales everything. Drawn additively without textures (see WorldSink.additive). Render thread only;
 * the meshes are static (no capturing lambdas, no allocation).
 */
public final class SkyFx {
    private static final int STARS = 160;
    private static final int GROUPS = 6;
    private static final int GLOW_LON = 48;
    private static final int AURORA_SEG = 120;
    private static final long PULSE_MIN_NANOS = 333_000_000L;
    private static final long FLASH_MIN_NANOS = 2_500_000_000L;


    // star field (fixed, generated once)
    private static final float[] SX = new float[STARS];
    private static final float[] SY = new float[STARS];
    private static final float[] SZ = new float[STARS];
    private static final float[] SSIZE = new float[STARS];
    private static final float[] SPHASE = new float[STARS];
    private static final float[] SRATE = new float[STARS];

    static {
        for (int i = 0; i < STARS; i++) {
            double lon = hash(i, 1) * Math.PI * 2;
            double elev = Math.toRadians(12 + 74 * Math.pow(hash(i, 2), 0.8));
            float ce = (float) Math.cos(elev);
            SX[i] = (float) (ce * Math.sin(lon));
            SY[i] = (float) Math.sin(elev);
            SZ[i] = (float) (ce * Math.cos(lon));
            SSIZE[i] = 0.0045f + 0.0065f * hash(i, 3);
            SPHASE[i] = hash(i, 4) * 6.2832f;
            SRATE[i] = 0.6f + 1.6f * hash(i, 5);
        }
    }

    // smoothed state
    private static float bassS;
    private static float energyS;
    private static float midS;
    private static float highS;
    private static float pulse;
    private static float burst;
    private static int burstGroup;
    private static float bloom;
    private static float hueT;
    private static int seenOnsets;
    private static int seenDrops;
    private static long lastPulseNanos;
    private static long lastFlashNanos = Long.MIN_VALUE / 2;
    private static long lastNanos;
    private static float time;

    // per frame inputs of the meshes
    private static float strength;
    private static float radius;
    private static boolean night;
    private static boolean auroraSky;
    private static int glowColor;
    private static int starColor;
    private static int auroraLow;
    private static int auroraHigh;
    private static float glowAlpha;
    private static float flashAlpha;
    private static float starBase;
    private static float auroraAlpha;
    private static boolean vanilla;

    // statistics (render thread)
    public static long frames;
    public static double frameMicrosAvg;
    public static double frameMicrosMax;
    public static long allocatedBytes;
    private static long frameNanos;
    private static final com.sun.management.ThreadMXBean MX = bean();

    private SkyFx() {}

    private static final WorldSink.Mesh GLOW = SkyFx::glow;
    private static final WorldSink.Mesh STARS_MESH = SkyFx::stars;
    private static final WorldSink.Mesh AURORA_MESH = SkyFx::aurora;

    /** Effects are on and something drives them (also false while the strength is 0). */
    public static boolean active() {
        LyricsConfig cfg = LyricsConfig.get();
        return cfg.skyFx && cfg.fxStrength > 0;
    }

    /**
     * Once per frame before the sky is drawn: eases the analysis values. Returns the color the custom sky's vertices
     * are multiplied with (the slow color drift), opaque white when nothing plays.
     */
    public static int update(SkyTheme theme) {
        long t0 = System.nanoTime();
        long alloc0 = MX != null ? MX.getCurrentThreadAllocatedBytes() : 0;
        long now = t0;
        float dt = lastNanos == 0 ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        time += dt;
        LyricsConfig cfg = LyricsConfig.get();
        boolean on = active() && MusicAnalyzer.source != MusicAnalyzer.Source.NONE;
        float k = on ? cfg.fxStrength / 100f : 0f;
        strength += (k - strength) * (1f - (float) Math.exp(-dt / 0.6f));
        bassS = ease(bassS, on ? MusicAnalyzer.bass : 0f, dt, 0.08f, 0.22f);
        energyS = ease(energyS, on ? MusicAnalyzer.energy : 0f, dt, 0.5f, 0.9f);
        midS = ease(midS, on ? MusicAnalyzer.mid : 0f, dt, 0.2f, 0.4f);
        highS = ease(highS, on ? MusicAnalyzer.high : 0f, dt, 0.15f, 0.35f);
        int o = MusicAnalyzer.onsets;
        if (o != seenOnsets) {
            seenOnsets = o;
            // never more than ~3 light-ups per second, whatever the music does
            if (on && now - lastPulseNanos > PULSE_MIN_NANOS) {
                lastPulseNanos = now;
                pulse = Math.max(pulse, 0.45f + 0.55f * MusicAnalyzer.onsetStrength);
                burst = 1f;
                burstGroup = (burstGroup + 1 + (o & 1)) % GROUPS;
            }
        }
        int d = MusicAnalyzer.drops;
        if (d != seenDrops) {
            seenDrops = d;
            if (on) {
                bloom = 1f;
                if (cfg.fxFlashes && now - lastFlashNanos > FLASH_MIN_NANOS) lastFlashNanos = now;
            }
        }
        pulse *= (float) Math.exp(-dt / 0.3f);
        burst *= (float) Math.exp(-dt / 0.5f);
        bloom *= (float) Math.exp(-dt / 1.4f);
        hueT += dt * (0.03f + 0.12f * energyS);

        float s = strength;
        int tint = 0xFFFFFFFF;
        if (theme != null) {
            float mixK = s * (0.06f + 0.22f * energyS + 0.06f * pulse);
            float w = 0.5f + 0.5f * (float) Math.sin(hueT);
            int target = LyricLayout.mix(theme.highlight | 0xFF000000, theme.glow | 0xFF000000, w);
            tint = LyricLayout.mix(0xFFFFFFFF, target | 0xFF000000, Math.min(0.4f, mixK)) | 0xFF000000;
            glowColor = LyricLayout.mix(theme.highlight | 0xFF000000, 0xFFFFFFFF, 0.25f);
            starColor = theme.night ? 0xFFF4F6FF : LyricLayout.mix(theme.highlight | 0xFF000000, 0xFFFFF4D6, 0.5f);
        } else {
            glowColor = 0xFFB9C7FF;
            starColor = 0xFFF4F6FF;
        }
        glowAlpha = s * (0.10f * bassS + 0.22f * pulse + 0.30f * bloom);
        // a soft flash: 120 ms in, half a second out, at most a third of the way to white
        float since = (now - lastFlashNanos) / 1e9f;
        float flash = since < 0f ? 0f : since < 0.12f ? since / 0.12f : (float) Math.exp(-(since - 0.12f) / 0.5f);
        flashAlpha = since > 3f ? 0f : s * 0.32f * flash;
        frameNanos = System.nanoTime() - t0;
        if (MX != null) allocatedBytes += Math.max(0, MX.getCurrentThreadAllocatedBytes() - alloc0);
        return tint;
    }

    /**
     * Draws the effects around the camera (pose origin = camera). {@code theme} null = the vanilla sky (then only
     * stars and aurora, and only at night).
     */
    public static void render(PoseStack pose, WorldSink sink, SkyTheme theme, float r, boolean isNight) {
        long t0 = System.nanoTime();
        long alloc0 = MX != null ? MX.getCurrentThreadAllocatedBytes() : 0;
        radius = r;
        night = isNight;
        vanilla = theme == null;
        auroraSky = theme == SkyTheme.AURORA;
        float s = strength;
        // the vanilla sky only gets the night effects (stars, aurora); its day stays untouched
        if (s > 0.01f && !(vanilla && !night)) {
            float shimmer = Math.max(midS, highS * 1.15f);
            if (auroraSky) {
                auroraAlpha = s * (0.30f + 0.70f * shimmer) + s * 0.25f * pulse;
                auroraLow = 0xFF5CFFB4;
                auroraHigh = 0xFF8C6BFF;
            } else if (night) {
                auroraAlpha = s * 0.75f * Math.max(0f, shimmer - 0.2f) + s * 0.15f * bloom;
                int hl = theme == null ? 0xFF7FD8FF : theme.highlight | 0xFF000000;
                auroraLow = LyricLayout.mix(hl, 0xFF5CFFB4, 0.35f);
                auroraHigh = LyricLayout.mix(hl, 0xFF8C6BFF, 0.5f);
            } else {
                auroraAlpha = 0f;
            }
            starBase = night ? s * (0.18f + 0.35f * energyS) : 0f;
            if (!vanilla && (glowAlpha > 0.003f || flashAlpha > 0.003f)) sink.additive(pose, GLOW, -999);
            if (auroraAlpha > 0.01f) sink.additive(pose, AURORA_MESH, -998);
            if (starBase > 0.01f || burst > 0.02f) sink.additive(pose, STARS_MESH, -997);
        }
        long took = System.nanoTime() - t0 + frameNanos;
        if (MX != null) allocatedBytes += Math.max(0, MX.getCurrentThreadAllocatedBytes() - alloc0);
        frames++;
        double us = took / 1000.0;
        frameMicrosAvg = frames == 1 ? us : frameMicrosAvg * 0.98 + us * 0.02;
        frameMicrosMax = Math.max(frameMicrosMax * 0.999, us);
    }

    /** One line for logs and tests (render side). */
    public static String stats() {
        return String.format(java.util.Locale.ROOT, "frames=%d avg=%.1fus max=%.1fus alloc=%dB strength=%.2f glow=%.2f aurora=%.2f",
                frames, frameMicrosAvg, frameMicrosMax, allocatedBytes, strength, glowAlpha, auroraAlpha);
    }

    // ============================================================================================ meshes
    // Additive, untextured quads (WorldSink.additive): soft fades stay soft and the drawing order does not matter.
    // Every quad is emitted in both windings so culling never hides it.

    /** Horizon glow (and the soft flash) on the inside of the dome. */
    private static void glow(Matrix4f m, VertexConsumer vc) {
        long t0 = System.nanoTime();
        float r = radius * 0.985f;
        float a = Math.min(0.6f, glowAlpha);
        if (a > 0.003f) {
            for (int j = 0; j < RINGS.length - 1; j++) {
                band(vc, m, r, RINGS[j], RINGS[j + 1], withAlpha(glowColor, a * RING_A[j]), withAlpha(glowColor, a * RING_A[j + 1]));
            }
        }
        if (flashAlpha > 0.003f) {
            int fc = LyricLayout.mix(glowColor, 0xFFFFFFFF, 0.5f);
            band(vc, m, r * 0.99f, -6f, 8f, withAlpha(fc, 0f), withAlpha(fc, flashAlpha));
            band(vc, m, r * 0.99f, 8f, 60f, withAlpha(fc, flashAlpha), withAlpha(fc, flashAlpha * 0.7f));
            band(vc, m, r * 0.99f, 60f, 89.5f, withAlpha(fc, flashAlpha * 0.7f), withAlpha(fc, flashAlpha * 0.5f));
        }
        frameNanos += System.nanoTime() - t0;
    }

    // -6 degrees nothing, full at the horizon, then fading out up to 45 degrees
    private static final float[] RINGS = {-6f, 0f, 8f, 18f, 30f, 45f};
    private static final float[] RING_A = {0f, 1f, 0.8f, 0.4f, 0.12f, 0f};

    private static void band(VertexConsumer vc, Matrix4f m, float r, float e0, float e1, int c0, int c1) {
        for (int i = 0; i < GLOW_LON; i++) {
            float l0 = 360f * i / GLOW_LON;
            float l1 = 360f * (i + 1) / GLOW_LON;
            quadSphere(vc, m, r, l0, l1, e0, e0, e1, e1, c0, c0, c1, c1);
        }
    }

    /**
     * Stars that twinkle softly with the energy; on each beat one group of them lights up (warm motes on day skies).
     * Each is a small four-pointed sparkle: four tapered rays that are bright at the centre and fade to nothing.
     */
    private static void stars(Matrix4f m, VertexConsumer vc) {
        long t0 = System.nanoTime();
        float r = radius * 0.97f;
        for (int i = 0; i < STARS; i++) {
            boolean inBurst = i % GROUPS == burstGroup;
            if (!night && !inBurst) continue;
            float tw = 0.5f + 0.5f * (float) Math.sin(time * SRATE[i] + SPHASE[i]);
            float a = night ? starBase * (0.35f + 0.65f * tw) : 0f;
            if (inBurst) a += strength * 0.9f * burst;
            if (a < 0.02f) continue;
            a = Math.min(1f, a);
            float size = SSIZE[i] * 1.6f * (1f + (inBurst ? 0.8f * burst : 0f)) * r;
            float x = SX[i];
            float y = SY[i];
            float z = SZ[i];
            // a tangent basis at the star: east = up x dir, north = dir x east
            float ex = z;
            float ez = -x;
            float el = (float) Math.sqrt(ex * ex + ez * ez);
            if (el < 1e-4f) {
                ex = 1f;
                ez = 0f;
                el = 1f;
            }
            ex /= el;
            ez /= el;
            float nx = y * ez;
            float ny = z * ex - x * ez;
            float nz = -y * ex;
            int c = withAlpha(starColor, a);
            int c0 = starColor & 0xFFFFFF;
            float cx = x * r;
            float cy = y * r;
            float cz = z * r;
            float w = size * 0.18f;
            // four rays: +east, -east, +north, -north
            for (int k = 0; k < 4; k++) {
                float dx = k < 2 ? ex : nx;
                float dy = k < 2 ? 0f : ny;
                float dz = k < 2 ? ez : nz;
                float qx = k < 2 ? nx : ex;
                float qy = k < 2 ? ny : 0f;
                float qz = k < 2 ? nz : ez;
                float sign = (k & 1) == 0 ? 1f : -1f;
                float tx = cx + dx * size * sign;
                float ty = cy + dy * size * sign;
                float tz = cz + dz * size * sign;
                quad(vc, m,
                        cx + qx * w, cy + qy * w, cz + qz * w, c,
                        cx - qx * w, cy - qy * w, cz - qz * w, c,
                        tx - qx * w * 0.1f, ty - qy * w * 0.1f, tz - qz * w * 0.1f, c0,
                        tx + qx * w * 0.1f, ty + qy * w * 0.1f, tz + qz * w * 0.1f, c0);
            }
        }
        frameNanos += System.nanoTime() - t0;
    }

    /** Two slow aurora curtains whose brightness follows the mids and highs. */
    private static void aurora(Matrix4f m, VertexConsumer vc) {
        long t0 = System.nanoTime();
        float r = radius * 0.975f;
        curtain(vc, m, r, 10f, 200f, 0f);
        curtain(vc, m, r, 215f, 120f, 2.1f);
        frameNanos += System.nanoTime() - t0;
    }

    private static void curtain(VertexConsumer vc, Matrix4f m, float r, float from, float span, float seed) {
        float a = Math.min(0.55f, auroraAlpha);
        float lift = 0.6f + 0.4f * midS;
        for (int i = 0; i < AURORA_SEG; i++) {
            float f0 = i / (float) AURORA_SEG;
            float f1 = (i + 1) / (float) AURORA_SEG;
            float l0 = from + span * f0;
            float l1 = from + span * f1;
            float b0 = 15f + 6f * (float) Math.sin(Math.toRadians(l0 * 2) + time * 0.10f + seed);
            float b1 = 15f + 6f * (float) Math.sin(Math.toRadians(l1 * 2) + time * 0.10f + seed);
            float h0 = (16f + 9f * (float) Math.sin(Math.toRadians(l0 * 3) - time * 0.15f + seed)) * lift;
            float h1 = (16f + 9f * (float) Math.sin(Math.toRadians(l1 * 3) - time * 0.15f + seed)) * lift;
            // fades in and out along the curtain, ripples move along it, fine vertical streaks
            float w0 = (float) Math.sin(Math.PI * f0) * (0.65f + 0.35f * (float) Math.sin(l0 * 0.09f + time * 0.6f + seed)) * streak(i, seed);
            float w1 = (float) Math.sin(Math.PI * f1) * (0.65f + 0.35f * (float) Math.sin(l1 * 0.09f + time * 0.6f + seed)) * streak(i + 1, seed);
            // soft base -> bright band just above it -> fades out at the top
            float k0 = b0 + h0 * 0.18f;
            float k1 = b1 + h1 * 0.18f;
            float m0 = b0 + h0 * 0.55f;
            float m1 = b1 + h1 * 0.55f;
            int base = auroraLow & 0xFFFFFF;
            int top = auroraHigh & 0xFFFFFF;
            int band0 = withAlpha(auroraLow, a * w0);
            int band1 = withAlpha(auroraLow, a * w1);
            int mid0 = withAlpha(auroraHigh, a * w0 * 0.45f);
            int mid1 = withAlpha(auroraHigh, a * w1 * 0.45f);
            quadSphere(vc, m, r, l0, l1, b0, b1, k0, k1, base, base, band0, band1);
            quadSphere(vc, m, r, l0, l1, k0, k1, m0, m1, band0, band1, mid0, mid1);
            quadSphere(vc, m, r, l0, l1, m0, m1, b0 + h0, b1 + h1, mid0, mid1, top, top);
        }
    }

    private static float streak(int i, float seed) {
        return 0.55f + 0.45f * hash(i, (long) (seed * 10) + 11);
    }

    // ============================================================================================ helpers

    /** A quad on the sphere between longitudes l0/l1, bottom edge at e00/e01, top edge at e10/e11. */
    private static void quadSphere(VertexConsumer vc, Matrix4f m, float r, float l0, float l1, float e00, float e01, float e10, float e11,
                                   int c00, int c01, int c10, int c11) {
        quad(vc, m,
                sx(l0, e00, r), sy(e00, r), sz(l0, e00, r), c00,
                sx(l1, e01, r), sy(e01, r), sz(l1, e01, r), c01,
                sx(l1, e11, r), sy(e11, r), sz(l1, e11, r), c11,
                sx(l0, e10, r), sy(e10, r), sz(l0, e10, r), c10);
    }

    private static float sx(float lonDeg, float elevDeg, float r) {
        return (float) (Math.cos(Math.toRadians(elevDeg)) * Math.sin(Math.toRadians(lonDeg))) * r;
    }

    private static float sy(float elevDeg, float r) {
        return (float) Math.sin(Math.toRadians(elevDeg)) * r;
    }

    private static float sz(float lonDeg, float elevDeg, float r) {
        return (float) (Math.cos(Math.toRadians(elevDeg)) * Math.cos(Math.toRadians(lonDeg))) * r;
    }

    /** One quad in both windings. */
    private static void quad(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, int ac, float bx, float by, float bz, int bc,
                             float cx, float cy, float cz, int cc, float dx, float dy, float dz, int dc) {
        put(vc, m, ax, ay, az, ac);
        put(vc, m, bx, by, bz, bc);
        put(vc, m, cx, cy, cz, cc);
        put(vc, m, dx, dy, dz, dc);
        put(vc, m, dx, dy, dz, dc);
        put(vc, m, cx, cy, cz, cc);
        put(vc, m, bx, by, bz, bc);
        put(vc, m, ax, ay, az, ac);
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, int color) {
        vc.addVertex(m, x, y, z).setColor(color);
    }

    private static int withAlpha(int rgb, float a) {
        int al = Math.round(255 * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static float ease(float cur, float target, float dt, float up, float down) {
        float tau = target > cur ? up : down;
        return cur + (target - cur) * (1f - (float) Math.exp(-dt / tau));
    }

    private static float hash(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    private static com.sun.management.ThreadMXBean bean() {
        try {
            var b = java.lang.management.ManagementFactory.getThreadMXBean();
            if (b instanceof com.sun.management.ThreadMXBean sun && sun.isThreadAllocatedMemorySupported()) return sun;
        } catch (Throwable ignored) {
            // not on this JVM
        }
        return null;
    }

    /** Test hook: what the sky shows right now {strength, glow, flash, aurora, pulse}. */
    public static float[] snapshot() {
        return new float[]{strength, glowAlpha, flashAlpha, auroraAlpha, pulse};
    }
}
