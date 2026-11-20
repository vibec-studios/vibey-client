package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.clips.shots.ShotFiles;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Locale;

final class GalleryFixtures {
    static final int COUNT = 7;
    private static final long BASE = 1_760_000_000_000L;
    private static final int[][] COLORS = {{0x8B6CFF, 0x3FE0C5}, {0xFF5470, 0xFFB547}, {0x2B7BFF, 0x0B0B0F}, {0x3FE0C5, 0x14532D},
            {0xFFB547, 0x8B6CFF}, {0xF4F4F5, 0x6B6B75}, {0x0EA5E9, 0xFF5470}};

    private GalleryFixtures() {}

    static void write() {
        try {
            Path dir = ShotFiles.dir();
            Files.createDirectories(dir);
            for (int i = 0; i < COUNT; i++) {
                Path f = dir.resolve(String.format(Locale.ROOT, "2025-10-%02d_12.00.00.png", 10 + i));
                ImageIO.write(image(i), "png", f.toFile());
                Files.setLastModifiedTime(f, FileTime.fromMillis(BASE + i * 86_400_000L));
            }
        } catch (IOException e) {
            throw new AssertionError("fixtures", e);
        }
    }

    private static BufferedImage image(int i) {
        BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(COLORS[i][0]), 1280, 720, new Color(COLORS[i][1])));
        g.fillRect(0, 0, 1280, 720);
        g.setColor(new Color(255, 255, 255, 220));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 360));
        String s = Integer.toString(i + 1);
        g.drawString(s, 640 - g.getFontMetrics().stringWidth(s) / 2, 480);
        g.dispose();
        return img;
    }
}
