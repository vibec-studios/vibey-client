package dev.henny.vibeyclient.ui.toast;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player heads (8×8 face + hat layer) for lists and toasts. Skins are fetched and decoded on a worker thread into
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
    private static volatile HttpClient http;

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

    private static void load(String key) {
        if (!LOADING.add(key)) return;
        Tasks.run(() -> {
            try {
                NativeImage img;
                if (key.startsWith("default:")) {
                    var rm = Minecraft.getInstance().getResourceManager();
                    var res = rm.getResource(Compat.id("minecraft", "textures/entity/player/wide/" + key.substring(8) + ".png"));
                    if (res.isEmpty()) throw new IllegalStateException("no default skin " + key);
                    try (InputStream in = res.get().open()) {
                        img = NativeImage.read(in);
                    }
                } else {
                    img = NativeImage.read(fetch(key));
                }
                try {
                    FACES.put(key, face(img));
                } finally {
                    img.close();
                }
            } catch (Exception e) {
                FAILED.add(key);
                dev.henny.vibeyclient.Vibey.LOG.debug("Skin {} failed: {}", key, e.toString());
            } finally {
                LOADING.remove(key);
            }
        });
    }

    private static byte[] fetch(String url) throws Exception {
        if (url.startsWith("http://textures.minecraft.net/")) url = "https://" + url.substring(7);
        if (!url.startsWith("https://") && !url.startsWith("http://")) throw new IllegalArgumentException("bad url");
        HttpClient c = http;
        if (c == null) {
            c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL)
                    .executor(Tasks.executor()).build();
            http = c;
        }
        HttpResponse<byte[]> res = c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200 || res.body().length > MAX_BYTES) throw new IllegalStateException("HTTP " + res.statusCode());
        return res.body();
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
