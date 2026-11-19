package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.avatar.render.Tex;
import dev.henny.vibeyclient.ui.Gfx;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Thumbnail textures by source (classpath path or URL): loaded on a worker, uploaded on the render thread. */
final class ThumbnailCache {
    private ThumbnailCache() {}

    private static final Object LOADING = new Object();
    private static final Object FAILED = new Object();
    private static final Map<String, Object> STATE = new HashMap<>();
    private static final Map<String, byte[]> READY = new ConcurrentHashMap<>();
    /** Own thread: catalog downloads must not starve the thumbnails. */
    private static final java.util.concurrent.ExecutorService LOADER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VibeyClient-Thumbnails");
        t.setDaemon(true);
        return t;
    });

    /** Thumbnails of dev items (devlink pushes): key per id, PNG per key. Client thread. */
    private static final Map<String, String> DEV_KEYS = new HashMap<>();
    private static final Map<String, byte[]> DEV_PNGS = new HashMap<>();
    private static int devCounter;

    /** Registers a pushed thumbnail for dev item {@code id}; returns the key to use as the item's thumbnail. */
    static String putDev(String id, byte[] png) {
        removeDev(id);
        String key = "dev:" + id + "#" + (++devCounter);
        DEV_KEYS.put(id, key);
        DEV_PNGS.put(key, png);
        return key;
    }

    /** The current thumbnail key of dev item {@code id}, or null. */
    static String devKey(String id) {
        return DEV_KEYS.get(id);
    }

    /** Forgets (and releases) the thumbnail of dev item {@code id}. */
    static void removeDev(String id) {
        String old = DEV_KEYS.remove(id);
        if (old == null) return;
        DEV_PNGS.remove(old);
        READY.remove(old);
        if (STATE.remove(old) instanceof Tex t) t.release();
    }

    static boolean draw(Gfx g, AvatarItem item, int x, int y, int size) {
        // dev items show their pushed thumbnail; the builtin thumbnail in the jar is instant; catalog URLs only for
        // items the jar does not have
        AvatarItem builtin = AvatarItems.get(item.id(), ItemSource.BUILTIN);
        String src = item.isDev() && item.thumbnail() != null ? item.thumbnail()
                : builtin != null && builtin.thumbnail() != null ? builtin.thumbnail() : item.thumbnail();
        if (src == null) return false;
        Object s = STATE.get(src);
        if (s == null && src.startsWith("dev:")) {
            byte[] png = DEV_PNGS.get(src);
            if (png == null) return false;
            STATE.put(src, LOADING);
            READY.put(src, png);
            s = LOADING;
        }
        if (s == null) {
            STATE.put(src, LOADING);
            LOADER.execute(() -> {
                if (!src.startsWith("/")) {
                    // disk cache read here, downloads asynchronously (several at a time, no thread waits)
                    ItemLoader.thumbnail(src).whenComplete((png, e) -> {
                        if (e != null) dev.henny.vibeyclient.Vibey.LOG.debug("Thumbnail {} failed: {}", src, e.toString());
                        READY.put(src, png == null ? new byte[0] : png);
                    });
                    return;
                }
                byte[] png = null;
                try (InputStream in = ThumbnailCache.class.getResourceAsStream(src)) {
                    if (in != null) png = in.readAllBytes();
                } catch (Exception e) {
                    dev.henny.vibeyclient.Vibey.LOG.debug("Thumbnail {} failed: {}", src, e.toString());
                }
                READY.put(src, png == null ? new byte[0] : png);
            });
            return false;
        }
        if (s == LOADING) {
            byte[] png = READY.remove(src);
            if (png == null) return false;
            if (png.length == 0) {
                STATE.put(src, FAILED);
                return false;
            }
            try {
                s = Tex.upload("thumb", png);
            } catch (Exception e) {
                s = FAILED;
            }
            STATE.put(src, s);
        }
        if (!(s instanceof Tex t) || t.released()) return false;
        g.image(t.id, x, y, size, size, 0, 0, t.width, t.height, t.width, t.height);
        return true;
    }
}
