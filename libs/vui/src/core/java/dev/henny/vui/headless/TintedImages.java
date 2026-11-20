package dev.henny.vui.headless;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

final class TintedImages {
    private final Map<BufferedImage, Map<Integer, BufferedImage>> cache = new HashMap<>();

    BufferedImage get(BufferedImage source, int argb) {
        if (argb == 0xFFFFFFFF) return source;
        return cache.computeIfAbsent(source, s -> new HashMap<>()).computeIfAbsent(argb, t -> tint(source, t));
    }

    void clear() {
        cache.clear();
    }

    static BufferedImage tint(BufferedImage src, int argb) {
        int ta = argb >>> 24;
        int tr = (argb >> 16) & 0xFF;
        int tg = (argb >> 8) & 0xFF;
        int tb = argb & 0xFF;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = (p >>> 24) * ta / 255;
                int r = ((p >> 16) & 0xFF) * tr / 255;
                int g = ((p >> 8) & 0xFF) * tg / 255;
                int b = (p & 0xFF) * tb / 255;
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }
}
