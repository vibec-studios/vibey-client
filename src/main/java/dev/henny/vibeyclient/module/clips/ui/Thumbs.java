package dev.henny.vibeyclient.module.clips.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vibeyclient.Vibey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clip thumbnails for the gallery: the JPEG is decoded on a background thread (only for tiles that are drawn), the
 * texture is made on the client thread, and everything is released when the gallery closes.
 */
final class Thumbs {
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey clips thumbnails");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static final AtomicInteger IDS = new AtomicInteger();

    record Tex(Identifier id, int w, int h) {}

    private record Decoded(int w, int h, int[] argb) {}

    private final Map<String, Tex> ready = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final Map<String, Decoded> decoded = new HashMap<>();
    private volatile boolean closed;

    /** The texture for {@code key} (a clip id), or null while it loads / when there is none. Client thread. */
    Tex get(String key, Path jpg) {
        closed = false;
        Tex t = ready.get(key);
        if (t != null || jpg == null) return t;
        Decoded d;
        synchronized (decoded) {
            d = decoded.remove(key);
        }
        if (d != null) {
            try {
                NativeImage img = new NativeImage(d.w, d.h, false);
                for (int y = 0; y < d.h; y++) for (int x = 0; x < d.w; x++) img.setPixel(x, y, d.argb[y * d.w + x]);
                DynamicTexture tex = new DynamicTexture(() -> "vibeyclient clip thumbnail", img);
                Identifier id = Identifier.fromNamespaceAndPath("vibeyclient", "clips/thumb_" + IDS.incrementAndGet());
                Minecraft.getInstance().getTextureManager().register(id, tex);
                t = new Tex(id, d.w, d.h);
                ready.put(key, t);
                return t;
            } catch (RuntimeException e) {
                Vibey.LOG.debug("Clips: thumbnail upload failed: {}", e.toString());
                ready.put(key, null);
            }
        }
        if (loading.add(key)) {
            LOADER.execute(() -> {
                if (closed) return;
                try {
                    BufferedImage bi = ImageIO.read(jpg.toFile());
                    if (bi == null) return;
                    int w = bi.getWidth(), h = bi.getHeight();
                    int[] px = bi.getRGB(0, 0, w, h, null, 0, w);
                    for (int i = 0; i < px.length; i++) px[i] |= 0xFF000000;
                    synchronized (decoded) {
                        if (!closed) decoded.put(key, new Decoded(w, h, px));
                    }
                } catch (Exception e) {
                    Vibey.LOG.debug("Clips: thumbnail {} unreadable: {}", jpg, e.toString());
                }
            });
        }
        return null;
    }

    /** Forget one (the clip was deleted). */
    void drop(String key) {
        Tex t = ready.remove(key);
        if (t != null) Minecraft.getInstance().getTextureManager().release(t.id());
        loading.remove(key);
    }

    void close() {
        closed = true;
        for (Tex t : ready.values()) if (t != null) Minecraft.getInstance().getTextureManager().release(t.id());
        ready.clear();
        synchronized (decoded) {
            decoded.clear();
        }
        loading.clear();
    }
}
