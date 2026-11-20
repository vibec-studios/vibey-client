package dev.henny.vibeyclient.ui.toast;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.platform.NativeImage;

import dev.henny.vibeyclient.backend.net.Net;
import dev.henny.vibeyclient.backend.net.SingleFlight;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player heads (8×8 face + hat layer) for lists and toasts. Skins are fetched (shared client) and decoded on a worker thread into
 * 64 ARGB values and drawn as plain rectangles – no textures to register, identical on every Minecraft version.
 * Players without a known skin get the vanilla default skin of their UUID.
 */
public final class Heads {
    private Heads() {}

    private static final String[] DEFAULTS = {"alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri"};
    private static final int MAX_BYTES = 1 << 20;

    private static final Map<String, int[]> FACES = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> SKIN_OF_USER = new ConcurrentHashMap<>();

    /** Remembers the skin URL of a user, so heads drawn by id alone (toasts) find it. */
    public static void remember(String userId, String skinUrl) {
        if (userId != null && skinUrl != null) SKIN_OF_USER.put(userId, skinUrl);
    }

    public static String skinOf(String userId) {
        return userId == null ? null : SKIN_OF_USER.get(userId);
    }

    /** Draws the head of {@code userId} ({@code skinUrl} may be null: remembered one or the default skin). */
    public static void draw(Gfx g, String userId, String skinUrl, int x, int y, int size) {
        if (skinUrl == null) skinUrl = skinOf(userId);
        int[] face = null;
        if (skinUrl != null) {
            face = FACES.get(skinUrl);
            if (face == null && !FAILED.contains(skinUrl)) load(skinUrl);
        }
        if (face == null) {
            String key = "default:" + defaultName(userId);
            face = FACES.get(key);
            if (face == null) load(key);
        }
        if (face == null) {
            g.rect(x, y, size, size, Theme.SURFACE3);
            return;
        }
        drawFace(g, face, x, y, size);
    }

    public static int[] face(String userId, String skinUrl) {
        if (skinUrl == null) skinUrl = skinOf(userId);
        if (skinUrl != null) {
            int[] face = FACES.get(skinUrl);
            if (face != null) return face;
            if (!FAILED.contains(skinUrl)) {
                load(skinUrl);
                return null;
            }
        }
        String key = "default:" + defaultName(userId);
        int[] face = FACES.get(key);
        if (face == null) load(key);
        return face;
    }

    public static void drawFace(Gfx g, int[] face, int x, int y, int size) {
        for (int py = 0; py < 8; py++) {
            int y1 = y + py * size / 8;
            int y2 = y + (py + 1) * size / 8;
            for (int px = 0; px < 8; px++) {
                int c = face[py * 8 + px];
                if ((c >>> 24) == 0) continue;
                g.fill(x + px * size / 8, y1, x + (px + 1) * size / 8, y2, c);
            }
        }
    }

    static String defaultName(String userId) {
        int idx = 6; // steve
        if (userId != null) {
            try {
                idx = Math.floorMod(UUID.fromString(userId).hashCode(), 18) % 9;
            } catch (IllegalArgumentException e) {
                idx = Math.floorMod(userId.hashCode(), 9);
            }
        }
        return DEFAULTS[idx];
    }

    /**
     * Default skins are read from the game's resources on a worker; real skins are downloaded asynchronously on the
     * shared client (no worker waits for the network – blocking downloads on the {@code Tasks} pool used to stall
     * every request of the client when a friends list showed three or more heads) and only decoded on a worker.
     */
    private static void load(String key) {
        if (!LOADING.add(key)) return;
        if (key.startsWith("default:")) {
            Tasks.run(() -> decode(key, () -> {
                var rm = Minecraft.getInstance().getResourceManager();
                var res = rm.getResource(Compat.id("minecraft", "textures/entity/player/wide/" + key.substring(8) + ".png"));
                if (res.isEmpty()) throw new IllegalStateException("no default skin " + key);
                try (InputStream in = res.get().open()) {
                    return NativeImage.read(in);
                }
            }));
            return;
        }
        CompletableFuture<byte[]> bytes;
        try {
            bytes = fetch(key);
        } catch (Exception e) {
            bytes = CompletableFuture.failedFuture(e);
        }
        bytes.whenComplete((b, e) -> {
            if (e != null) {
                failed(key, e);
                return;
            }
            Tasks.run(() -> decode(key, () -> NativeImage.read(b)));
        });
    }

    private interface ImageSource {
        NativeImage open() throws Exception;
    }

    private static void decode(String key, ImageSource src) {
        try {
            NativeImage img = src.open();
            try {
                FACES.put(key, face(img));
            } finally {
                img.close();
            }
            LOADING.remove(key);
        } catch (Exception e) {
            failed(key, e);
        }
    }

    private static void failed(String key, Throwable e) {
        FAILED.add(key);
        LOADING.remove(key);
        dev.henny.vibeyclient.Vibey.LOG.debug("Skin {} failed: {}", key, Net.cause(e).toString());
    }

    private static final SingleFlight<String, byte[]> FLIGHT = new SingleFlight<>();

    private static CompletableFuture<byte[]> fetch(String url) {
        if (url.startsWith("http://textures.minecraft.net/")) url = "https://" + url.substring(7);
        if (!url.startsWith("https://") && !url.startsWith("http://")) throw new IllegalArgumentException("bad url");
        String u = url;
        return FLIGHT.run(u, () -> Net.send(Net.request(u, Net.Kind.API).GET().build()).thenApply(res -> {
            if (res.status() != 200 || res.body().length > MAX_BYTES) throw new IllegalStateException("HTTP " + res.status());
            return res.body();
        }));
    }

    /** Face (8,8)–(16,16) with the hat layer (40,8)–(48,16) on top, scaled for HD skins. */
    static int[] face(NativeImage img) {
        if (img.getWidth() < 64 || img.getHeight() < 32) throw new IllegalStateException("not a skin");
        int s = Math.max(1, img.getWidth() / 64);
        int[] out = new int[64];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int base = argb(img, (8 + x) * s, (8 + y) * s) | 0xFF000000;
                int hat = argb(img, (40 + x) * s, (8 + y) * s);
                int a = hat >>> 24;
                out[y * 8 + x] = a == 0 ? base : a == 255 ? hat : Theme.mix(base, hat | 0xFF000000, a / 255f);
            }
        }
        return out;
    }

    private static int argb(NativeImage img, int x, int y) {
        //? if >=1.21.2 {
        return img.getPixel(x, y);
        //?} else {
        /*int abgr = img.getPixelRGBA(x, y);
        return (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
        *///?}
    }
}
