package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.clips.shots.core.Downscale;
import dev.henny.vibeyclient.module.clips.shots.core.Shot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class ShotDecoder {
    static final int THUMB_W = 192, THUMB_H = 108;
    static final int PREVIEW_W = 960, PREVIEW_H = 540;

    record Pixels(int w, int h, int[] argb) {}

    private ShotDecoder() {}

    static Pixels thumb(Shot s) {
        Path cached = ShotFiles.cacheDir().resolve(hash(s.key()) + ".png");
        Pixels p = read(cached);
        if (p != null && p.w == THUMB_W && p.h == THUMB_H) return p;
        Pixels full = read(ShotFiles.file(s));
        if (full == null) return null;
        Pixels t = new Pixels(THUMB_W, THUMB_H, Downscale.cover(full.argb, full.w, full.h, THUMB_W, THUMB_H));
        write(cached, t);
        return t;
    }

    static Pixels preview(Shot s) {
        Pixels full = read(ShotFiles.file(s));
        if (full == null) return null;
        return new Pixels(PREVIEW_W, PREVIEW_H, Downscale.contain(full.argb, full.w, full.h, PREVIEW_W, PREVIEW_H));
    }

    private static Pixels read(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            BufferedImage img = ImageIO.read(file.toFile());
            if (img == null) return null;
            int w = img.getWidth(), h = img.getHeight();
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            for (int i = 0; i < px.length; i++) px[i] |= 0xFF000000;
            return new Pixels(w, h, px);
        } catch (IOException | RuntimeException e) {
            Vibey.LOG.debug("Screenshots: {} unreadable: {}", file, e.toString());
            return null;
        }
    }

    private static void write(Path file, Pixels p) {
        try {
            Files.createDirectories(file.getParent());
            BufferedImage img = new BufferedImage(p.w, p.h, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, p.w, p.h, p.argb, 0, p.w);
            ImageIO.write(img, "png", file.toFile());
        } catch (IOException | RuntimeException e) {
            Vibey.LOG.debug("Screenshots: thumbnail cache {} not written: {}", file, e.toString());
        }
    }

    private static String hash(String key) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(key.hashCode());
        }
    }
}
