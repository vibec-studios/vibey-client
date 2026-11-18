package dev.henny.vibeyclient.module.media.lyrics.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vibeyclient.module.media.lyrics.LyricsCompat;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.LyricsFeature;
import dev.henny.vibeyclient.module.media.lyrics.library.Song;
import dev.henny.vibeyclient.module.media.lyrics.pc.NowPlaying;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else
/*import net.minecraft.resources.ResourceLocation;*/

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The album art of what is playing: from the PC player (Windows media controls, Spotify on macOS, MPRIS on Linux) or
 * an image next to a song in the folder ({@code <song>.png/.jpg} or {@code cover.png/.jpg}). Loaded in the background,
 * shrunk to {@link #SIZE} pixels and turned into a texture; its main colors can tint the lyrics.
 */
public final class Cover {
    public static final int SIZE = 64;
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vibey Lyrics cover");
        t.setDaemon(true);
        return t;
    });
    private static HttpClient http;

    /** What should be shown ("" = nothing) and what is shown. */
    private static String wanted = "";
    private static String shownKey = "";
    private static int counter;
    //? if >=1.21.11 {
    private static Identifier shown;
    //?} else
    /*private static ResourceLocation shown;*/
    private static int[] colors;

    private static final Object LOCK = new Object();
    private static String readyKey;
    private static byte[] readyPng;
    private static int[] readyColors;

    // local songs: the image found for the current song
    private static String songId = "";
    private static String songCover = "";

    private Cover() {}

    //? if >=1.21.11 {
    public static Identifier texture() {
        return shown;
    }
    //?} else {
    /*public static ResourceLocation texture() {
        return shown;
    }
    *///?}

    /** Text, highlight and glow color taken from the art, or null. */
    public static int[] colors() {
        return shown == null ? null : colors;
    }

    /** Once per client tick (render thread): picks the source, starts loading it, uploads a finished image. */
    public static void tick() {
        String source = source();
        if (!source.equals(wanted)) {
            wanted = source;
            if (source.isEmpty()) {
                drop();
            } else {
                String key = source;
                LOADER.execute(() -> load(key));
            }
        }
        String key;
        byte[] png;
        int[] c;
        synchronized (LOCK) {
            key = readyKey;
            png = readyPng;
            c = readyColors;
            readyKey = null;
            readyPng = null;
        }
        if (key == null || !key.equals(wanted)) return;
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
            drop();
            //? if >=1.21.5 {
            DynamicTexture tex = new DynamicTexture(() -> "vibeyclient lyrics cover", img);
            //?} else
            /*DynamicTexture tex = new DynamicTexture(img);*/
            var id = LyricsCompat.id("vibeyclient", "lyrics/cover/" + (++counter));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            shown = id;
            shownKey = key;
            colors = c;
        } catch (Exception e) {
            LyricsFeature.LOG.debug("Cover upload failed: {}", e.toString());
        }
    }

    private static void drop() {
        if (shown != null) Minecraft.getInstance().getTextureManager().release(shown);
        shown = null;
        shownKey = "";
        colors = null;
    }

    private static String source() {
        Song song = Playback.song();
        if (song == null) return "";
        if (Playback.isPc()) {
            NowPlaying.Track t = NowPlaying.track();
            return t == null || t.cover() == null ? "" : t.cover();
        }
        if (!song.id().equals(songId)) {
            songId = song.id();
            songCover = findLocal(song);
        }
        return songCover;
    }

    private static String findLocal(Song song) {
        Path file = song.audio() != null ? song.audio() : song.lrc();
        if (file == null || file.getParent() == null) return "";
        Path dir = file.getParent();
        String base = song.baseName();
        for (String name : new String[]{base + ".png", base + ".jpg", base + ".jpeg", "cover.png", "cover.jpg", "cover.jpeg"}) {
            Path p = dir.resolve(name);
            if (Files.isRegularFile(p)) return p.toString();
        }
        return "";
    }

    // ============================================================================================= loading

    private static void load(String key) {
        try {
            BufferedImage src;
            try (InputStream in = open(key)) {
                src = ImageIO.read(in);
            }
            if (src == null) return;
            BufferedImage img = square(src);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            synchronized (LOCK) {
                readyKey = key;
                readyPng = out.toByteArray();
                readyColors = palette(img);
            }
        } catch (Exception e) {
            LyricsFeature.LOG.debug("Cover {} could not be read: {}", key, e.toString());
        }
    }

    private static InputStream open(String source) throws Exception {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(source)).timeout(Duration.ofSeconds(8)).GET().build();
            return http.send(req, HttpResponse.BodyHandlers.ofInputStream()).body();
        }
        Path p = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
        return Files.newInputStream(p);
    }

    /** The middle square (video thumbnails are 16:9), scaled to SIZE. */
    private static BufferedImage square(BufferedImage src) {
        int s = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - s) / 2;
        int y = (src.getHeight() - s) / 2;
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // Two steps keep big images smooth.
        if (s > SIZE * 4) {
            BufferedImage mid = new BufferedImage(SIZE * 4, SIZE * 4, BufferedImage.TYPE_INT_ARGB);
            Graphics2D m = mid.createGraphics();
            m.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            m.drawImage(src, 0, 0, SIZE * 4, SIZE * 4, x, y, x + s, y + s, null);
            m.dispose();
            g.drawImage(mid, 0, 0, SIZE, SIZE, null);
        } else {
            g.drawImage(src, 0, 0, SIZE, SIZE, x, y, x + s, y + s, null);
        }
        g.dispose();
        return out;
    }

    /**
     * The art's most striking color (hue buckets weighted by how colorful and bright each pixel is), as text,
     * highlight and glow color. Grey covers give soft white.
     */
    static int[] palette(BufferedImage img) {
        int buckets = 24;
        double[] weight = new double[buckets];
        double[] r = new double[buckets], gr = new double[buckets], b = new double[buckets];
        float[] hsb = new float[3];
        for (int y = 0; y < img.getHeight(); y += 2) {
            for (int x = 0; x < img.getWidth(); x += 2) {
                int p = img.getRGB(x, y);
                int pr = p >> 16 & 255, pg = p >> 8 & 255, pb = p & 255;
                java.awt.Color.RGBtoHSB(pr, pg, pb, hsb);
                double w = hsb[1] * hsb[1] * (0.2 + hsb[2]);
                if (hsb[2] < 0.15f) w = 0;
                int k = Math.min(buckets - 1, (int) (hsb[0] * buckets));
                weight[k] += w;
                r[k] += pr * w;
                gr[k] += pg * w;
                b[k] += pb * w;
            }
        }
        int best = 0;
        double total = 0;
        for (int k = 0; k < buckets; k++) {
            total += weight[k];
            if (weight[k] > weight[best]) best = k;
        }
        if (total < 1.5 || weight[best] <= 0) return new int[]{0xFFFFFFFF, 0xFFF2F2F2, 0xFF3A3A44};
        java.awt.Color.RGBtoHSB((int) (r[best] / weight[best]), (int) (gr[best] / weight[best]), (int) (b[best] / weight[best]), hsb);
        float hue = hsb[0];
        float sat = Math.max(0.35f, Math.min(0.85f, hsb[1]));
        int high = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, sat * 0.75f, 1f) & 0xFFFFFF;
        int glow = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, Math.min(1f, sat + 0.15f), 0.6f) & 0xFFFFFF;
        int text = LyricLayout.mix(0xFFFFFFFF, high, 0.15f);
        return new int[]{text, high, glow};
    }
}
