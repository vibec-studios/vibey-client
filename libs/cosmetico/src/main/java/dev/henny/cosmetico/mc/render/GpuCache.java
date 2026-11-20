package dev.henny.cosmetico.mc.render;

import dev.henny.cosmetico.mc.CosmeticoMod;
import dev.henny.cosmetico.mc.fx.EmbeddedSounds;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.skeleton.Rig;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GpuCache {
    private GpuCache() {}

    private static final int UPLOADS_PER_FRAME = 3;
    private static final long RELEASE_AFTER_MS = 30_000;

    private static final Map<Bundle, GpuBundle> CACHE = new IdentityHashMap<>();
    private static final Map<Bundle, Rig> RIGS = new ConcurrentHashMap<>();
    private static long frameMs;
    private static int uploadsThisFrame;
    private static long ticks;

    public static void prepare(Bundle b) {
        RIGS.computeIfAbsent(b, Rig::of);
    }

    public static GpuBundle get(Bundle b) {
        if (b == null) return null;
        GpuBundle g = CACHE.get(b);
        long now = System.currentTimeMillis();
        if (g == null) {
            g = create(b, now);
            if (g == null) return null;
        }
        g.lastUsed = now;
        return g;
    }

    private static GpuBundle create(Bundle b, long now) {
        if (now != frameMs) {
            frameMs = now;
            uploadsThisFrame = 0;
        }
        if (uploadsThisFrame >= UPLOADS_PER_FRAME) return null;
        uploadsThisFrame++;
        GpuBundle g;
        try {
            g = new GpuBundle(RIGS.computeIfAbsent(b, Rig::of));
        } catch (RuntimeException e) {
            CosmeticoMod.LOG.error("Could not prepare {}", b.id, e);
            return null;
        }
        CACHE.put(b, g);
        EmbeddedSounds.prefetch(b);
        g.animate(ticks, 0f);
        return g;
    }

    public static void tick() {
        ticks++;
        long now = System.currentTimeMillis();
        List<Bundle> drop = null;
        for (Iterator<Map.Entry<Bundle, GpuBundle>> it = CACHE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Bundle, GpuBundle> e = it.next();
            GpuBundle g = e.getValue();
            if (now - g.lastUsed > RELEASE_AFTER_MS) {
                g.release();
                it.remove();
                if (drop == null) drop = new ArrayList<>();
                drop.add(e.getKey());
            } else {
                g.animate(ticks, 0f);
            }
        }
        if (drop != null) for (Bundle b : drop) RIGS.remove(b);
    }

    public static void evict(Bundle b) {
        GpuBundle g = CACHE.remove(b);
        if (g != null) g.release();
        RIGS.remove(b);
    }

    public static int size() {
        return CACHE.size();
    }
}
