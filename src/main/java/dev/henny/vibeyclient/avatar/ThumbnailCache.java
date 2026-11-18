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

    static boolean draw(Gfx g, AvatarItem item, int x, int y, int size) {
        // the builtin thumbnail in the jar is instant; catalog URLs only for items the jar does not have
        AvatarItem builtin = AvatarItems.get(item.id(), ItemSource.BUILTIN);
        String src = builtin != null && builtin.thumbnail() != null ? builtin.thumbnail() : item.thumbnail();
        if (src == null) return false;
        Object s = STATE.get(src);
        if (s == null) {
            STATE.put(src, LOADING);
            LOADER.execute(() -> {
                byte[] png = null;
                try {
                    if (src.startsWith("/")) {
                        try (InputStream in = ThumbnailCache.class.getResourceAsStream(src)) {
                            if (in != null) png = in.readAllBytes();
                        }
                    } else {
                        png = ItemLoader.thumbnail(src);
                    }
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
