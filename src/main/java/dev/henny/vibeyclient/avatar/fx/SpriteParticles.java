package dev.henny.vibeyclient.avatar.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.render.BundleMesh;
import dev.henny.vibeyclient.avatar.render.GpuBundle;
import dev.henny.vibeyclient.avatar.render.GpuBundles;
import dev.henny.vibeyclient.avatar.render.MeshSink;
import dev.henny.vibeyclient.avatar.render.Tex;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Lightweight sprite particles from bundle textures ({@code sprites[]}), simulated per client tick and drawn as
 * camera-facing quads with the owner's world pass. Fixed-size pools per avatar, no allocation once warmed up.
 */
public final class SpriteParticles {
    private SpriteParticles() {}

    private static final int MAX = 256;
    private static final Map<AvatarState, Pool> POOLS = new IdentityHashMap<>();
    private static final Random RANDOM = new Random();

    private static final class Pool {
        final double[] x = new double[MAX], y = new double[MAX], z = new double[MAX];
        final double[] px = new double[MAX], py = new double[MAX], pz = new double[MAX];
        final float[] vx = new float[MAX], vy = new float[MAX], vz = new float[MAX];
        final float[] age = new float[MAX], life = new float[MAX], size = new float[MAX], gravity = new float[MAX];
        final Bundle[] bundle = new Bundle[MAX];
        final int[] sprite = new int[MAX];
        final boolean[] emissive = new boolean[MAX];
        int count;
        /** Quad buffers per frame, one per draw call (double buffered against the submit ring). */
        float[][] data = new float[8][];
    }

    public static void spawn(AvatarState owner, Bundle b, int spriteIndex, double x, double y, double z,
                             float vx, float vy, float vz, float spreadX, float spreadY, float spreadZ,
                             float lifetimeSec, float size, float gravity, boolean emissive) {
        Pool p = POOLS.computeIfAbsent(owner, k -> new Pool());
        if (p.count >= MAX) return;
        int i = p.count++;
        p.x[i] = p.px[i] = x + (RANDOM.nextFloat() * 2 - 1) * spreadX;
        p.y[i] = p.py[i] = y + (RANDOM.nextFloat() * 2 - 1) * spreadY;
        p.z[i] = p.pz[i] = z + (RANDOM.nextFloat() * 2 - 1) * spreadZ;
        // a little randomness so bursts do not look like one particle
        p.vx[i] = vx + (RANDOM.nextFloat() - 0.5f) * 0.01f;
        p.vy[i] = vy + (RANDOM.nextFloat() - 0.5f) * 0.01f;
        p.vz[i] = vz + (RANDOM.nextFloat() - 0.5f) * 0.01f;
        p.age[i] = 0;
        p.life[i] = Math.max(1f, lifetimeSec * 20f * (0.85f + RANDOM.nextFloat() * 0.3f));
        p.size[i] = size;
        p.gravity[i] = gravity;
        p.bundle[i] = b;
        p.sprite[i] = spriteIndex;
        p.emissive[i] = emissive;
    }

    /** 20/s: physics and expiry. */
    public static void tick() {
        for (java.util.Iterator<Pool> it = POOLS.values().iterator(); it.hasNext(); ) {
            Pool p = it.next();
            for (int i = 0; i < p.count; i++) {
                p.px[i] = p.x[i];
                p.py[i] = p.y[i];
                p.pz[i] = p.z[i];
                p.x[i] += p.vx[i];
                p.y[i] += p.vy[i];
                p.z[i] += p.vz[i];
                p.vy[i] -= p.gravity[i];
                p.vx[i] *= 0.98f;
                p.vy[i] *= 0.98f;
                p.vz[i] *= 0.98f;
                if (++p.age[i] >= p.life[i]) {
                    int last = --p.count;
                    move(p, last, i);
                    p.bundle[last] = null;
                    i--;
                }
            }
            if (p.count == 0) it.remove();
        }
    }

    private static void move(Pool p, int from, int to) {
        p.x[to] = p.x[from];
        p.y[to] = p.y[from];
        p.z[to] = p.z[from];
        p.px[to] = p.px[from];
        p.py[to] = p.py[from];
        p.pz[to] = p.pz[from];
        p.vx[to] = p.vx[from];
        p.vy[to] = p.vy[from];
        p.vz[to] = p.vz[from];
        p.age[to] = p.age[from];
        p.life[to] = p.life[from];
        p.size[to] = p.size[from];
        p.gravity[to] = p.gravity[from];
        p.bundle[to] = p.bundle[from];
        p.sprite[to] = p.sprite[from];
        p.emissive[to] = p.emissive[from];
    }

    public static void clear(AvatarState owner) {
        POOLS.remove(owner);
    }

    private static final Vector3f R = new Vector3f(), U = new Vector3f();

    /**
     * Draws the particles of {@code owner}; {@code ps} is at the owner's position {@code ox, oy, oz} (camera
     * relative, world axes).
     */
    public static void render(AvatarState owner, PoseStack ps, MeshSink sink, double ox, double oy, double oz, Quaternionf cam) {
        Pool p = POOLS.get(owner);
        if (p == null || p.count == 0) return;
        float pt = owner.partial;
        cam.transform(R.set(1, 0, 0));
        cam.transform(U.set(0, 1, 0));
        // group by bundle + sprite texture + emissive; few groups in practice
        int groups = 0;
        int done = 0;
        boolean[] used = USED.length >= p.count ? USED : (USED = new boolean[MAX]);
        java.util.Arrays.fill(used, 0, p.count, false);
        while (done < p.count && groups < p.data.length) {
            int first = -1;
            for (int i = 0; i < p.count; i++) if (!used[i]) { first = i; break; }
            if (first < 0) break;
            Bundle b = p.bundle[first];
            Bundle.Sprite sp = b.sprites[p.sprite[first]];
            int tex = sp.texture();
            boolean em = p.emissive[first];
            int n = 0;
            for (int i = first; i < p.count; i++) {
                if (used[i] || p.bundle[i] != b || b.sprites[p.sprite[i]].texture() != tex || p.emissive[i] != em) continue;
                n++;
            }
            float[] d = p.data[groups];
            int need = n * 4 * BundleMesh.STRIDE;
            if (d == null || d.length < need) d = p.data[groups] = new float[Math.max(need, 64 * 4 * BundleMesh.STRIDE)];
            int o = 0;
            for (int i = first; i < p.count; i++) {
                if (used[i] || p.bundle[i] != b || b.sprites[p.sprite[i]].texture() != tex || p.emissive[i] != em) continue;
                used[i] = true;
                done++;
                Bundle.Sprite s = b.sprites[p.sprite[i]];
                Bundle.Texture t = b.textures[tex];
                float u1 = s.u1() / t.width, v1 = s.v1() / t.height, u2 = s.u2() / t.width, v2 = s.v2() / t.height;
                float x = (float) (p.px[i] + (p.x[i] - p.px[i]) * pt - ox);
                float y = (float) (p.py[i] + (p.y[i] - p.py[i]) * pt - oy);
                float z = (float) (p.pz[i] + (p.z[i] - p.pz[i]) * pt - oz);
                float life = (p.age[i] + pt) / p.life[i];
                float h = p.size[i] * 0.5f * (life > 0.8f ? (1f - life) * 5f : 1f);
                o = corner(d, o, x, y, z, -h, -h, u1, v2);
                o = corner(d, o, x, y, z, h, -h, u2, v2);
                o = corner(d, o, x, y, z, h, h, u2, v1);
                o = corner(d, o, x, y, z, -h, h, u1, v1);
            }
            groups++;
            GpuBundle gb = GpuBundles.get(b);
            Tex tx = gb == null ? null : gb.texture(tex);
            if (tx == null || n == 0) continue;
            sink.quads(ps, tx.emissive(), d, n, em ? MeshSink.FULL_BRIGHT : MeshSink.FULL_BRIGHT, -1);
        }
    }

    private static boolean[] USED = new boolean[MAX];

    private static int corner(float[] d, int o, float x, float y, float z, float a, float b, float u, float v) {
        d[o] = x + R.x * a + U.x * b;
        d[o + 1] = y + R.y * a + U.y * b;
        d[o + 2] = z + R.z * a + U.z * b;
        d[o + 3] = u;
        d[o + 4] = v;
        d[o + 5] = 0;
        d[o + 6] = 1;
        d[o + 7] = 0;
        return o + BundleMesh.STRIDE;
    }
}
