package dev.henny.vibeyclient.avatar.render;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.avatar.bundle.Bundle;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache of {@link GpuBundle}s (render thread). Meshes are compiled on worker threads when a bundle is registered
 * ({@link #prepare}); textures are uploaded on first use, at most a few per frame so equipping many items at once does
 * not stutter. Bundles that are no longer used are released after a while.
 */
public final class GpuBundles {
    private GpuBundles() {}

    private static final int UPLOADS_PER_FRAME = 3;
    private static final long RELEASE_AFTER_MS = 30_000;

    private static final Map<Bundle, GpuBundle> CACHE = new IdentityHashMap<>();
    private static final Map<Bundle, BundleMesh> MESHES = new ConcurrentHashMap<>();
    private static long frameMs;
    private static int uploadsThisFrame;
    private static long ticks;
    private static float partial;

    /** Compile the mesh ahead of time (any thread). */
    public static void prepare(Bundle b) {
        MESHES.computeIfAbsent(b, BundleMesh::compile);
    }

    /** The GPU side of {@code b}, or null if it is not ready yet (try again next frame). Render thread. */
    public static GpuBundle get(Bundle b) {
        if (b == null) return null;
        GpuBundle g = CACHE.get(b);
        long now = System.currentTimeMillis();
        if (g == null) {
            if (now != frameMs) {
                frameMs = now;
                uploadsThisFrame = 0;
            }
            if (uploadsThisFrame >= UPLOADS_PER_FRAME) return null;
            uploadsThisFrame++;
            try {
                BundleMesh mesh = MESHES.computeIfAbsent(b, BundleMesh::compile);
                g = new GpuBundle(b, mesh);
            } catch (RuntimeException e) {
                Vibey.LOG.error("Could not prepare {}", b.id, e);
                return null;
            }
            CACHE.put(b, g);
            dev.henny.vibeyclient.avatar.fx.EmbeddedSounds.prefetch(b);
            g.animate(ticks, partial);
        }
        g.lastUsed = now;
        return g;
    }

    /** Once per client tick: animated textures, cleanup. */
    public static void tick() {
        ticks++;
        long now = System.currentTimeMillis();
        List<Bundle> drop = null;
        for (Iterator<Map.Entry<Bundle, GpuBundle>> it = CACHE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Bundle, GpuBundle> e = it.next();
            GpuBundle g = e.getValue();
            if (now - g.lastUsed > RELEASE_AFTER_MS) {
                Bundle key = e.getKey();
                g.release();
                it.remove();
                if (drop == null) drop = new ArrayList<>();
                drop.add(key);
            } else {
                g.animate(ticks, 0f);
            }
        }
        if (drop != null) for (Bundle b : drop) MESHES.remove(b);
    }

    /** Release a replaced bundle right away (hot reload). */
    public static void evict(Bundle b) {
        GpuBundle g = CACHE.remove(b);
        if (g != null) g.release();
        MESHES.remove(b);
    }

    public static long ticks() {
        return ticks;
    }
}
