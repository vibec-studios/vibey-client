package dev.henny.cosmetico.mc.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.fx.SpritePool;
import dev.henny.cosmetico.fx.SpriteQuads;
import dev.henny.cosmetico.mc.render.GpuBundle;
import dev.henny.cosmetico.mc.render.GpuCache;
import dev.henny.cosmetico.mc.render.MeshSink;
import dev.henny.cosmetico.mc.render.Tex;
import dev.henny.cosmetico.mesh.BundleMesh;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.state.Avatar;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

public final class SpriteRender {
    private SpriteRender() {}

    private static final int GROUPS = 8;
    private static final Map<SpritePool, float[][]> DATA = new WeakHashMap<>();
    private static final Vector3f R = new Vector3f(), U = new Vector3f();
    private static final boolean[] USED = new boolean[SpritePool.MAX];

    public static void render(Avatar owner, PoseStack ps, MeshSink sink, double ox, double oy, double oz, Quaternionf cam) {
        SpritePool p = Sprites.pools().peek(owner.uuid);
        if (p == null || p.count == 0) return;
        float[][] data = DATA.computeIfAbsent(p, k -> new float[GROUPS][]);
        cam.transform(R.set(1, 0, 0));
        cam.transform(U.set(0, 1, 0));
        Arrays.fill(USED, 0, p.count, false);
        int done = 0;
        for (int group = 0; done < p.count && group < GROUPS; group++) {
            int first = firstUnused(p);
            if (first < 0) break;
            done += group(p, first, data, group, ps, sink, owner.frame.partial, ox, oy, oz);
        }
    }

    private static int firstUnused(SpritePool p) {
        for (int i = 0; i < p.count; i++) if (!USED[i]) return i;
        return -1;
    }

    private static int group(SpritePool p, int first, float[][] data, int group, PoseStack ps, MeshSink sink, float pt,
                             double ox, double oy, double oz) {
        Bundle b = p.bundle[first];
        int tex = b.sprites[p.sprite[first]].texture();
        boolean em = p.emissive[first];
        int n = 0;
        for (int i = first; i < p.count; i++) if (same(p, i, b, tex, em)) n++;
        float[] d = data[group];
        int need = n * 4 * BundleMesh.STRIDE;
        if (d == null || d.length < need) d = data[group] = new float[Math.max(need, 64 * 4 * BundleMesh.STRIDE)];
        int o = 0;
        for (int i = first; i < p.count; i++) {
            if (!same(p, i, b, tex, em)) continue;
            USED[i] = true;
            o = SpriteQuads.quad(d, o, p, i, pt, ox, oy, oz, R, U);
        }
        GpuBundle gb = GpuCache.get(b);
        Tex tx = gb == null ? null : gb.texture(tex);
        if (tx != null && n > 0) sink.quads(ps, tx.emissive(), d, n, MeshSink.FULL_BRIGHT, -1);
        return n;
    }

    private static boolean same(SpritePool p, int i, Bundle b, int tex, boolean em) {
        return !USED[i] && p.bundle[i] == b && b.sprites[p.sprite[i]].texture() == tex && p.emissive[i] == em;
    }
}
