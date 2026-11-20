package dev.henny.vui.icon;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

public final class IconAtlas {
    public static final int PADDING = 1;

    private final int width;
    private final int height;
    private final int[] pixels;
    private final Map<PixelIcon, int[]> slots;

    private IconAtlas(int width, int height, int[] pixels, Map<PixelIcon, int[]> slots) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
        this.slots = slots;
    }

    public static IconAtlas build(Collection<PixelIcon> icons, int maxWidth) {
        Map<PixelIcon, int[]> slots = new IdentityHashMap<>();
        int x = PADDING;
        int y = PADDING;
        int rowHeight = 0;
        int width = 1;
        for (PixelIcon icon : icons) {
            if (slots.containsKey(icon)) continue;
            if (x + icon.width() + PADDING > maxWidth && x > PADDING) {
                x = PADDING;
                y += rowHeight + PADDING;
                rowHeight = 0;
            }
            slots.put(icon, new int[]{x, y});
            x += icon.width() + PADDING;
            rowHeight = Math.max(rowHeight, icon.height());
            width = Math.max(width, x);
        }
        int height = y + rowHeight + PADDING;
        int[] pixels = new int[width * height];
        for (Map.Entry<PixelIcon, int[]> e : slots.entrySet()) paint(e.getKey(), e.getValue(), pixels, width);
        return new IconAtlas(width, height, pixels, slots);
    }

    private static void paint(PixelIcon icon, int[] at, int[] pixels, int width) {
        for (int py = 0; py < icon.height(); py++) {
            for (int px = 0; px < icon.width(); px++) {
                if (icon.on(px, py)) pixels[(at[1] + py) * width + at[0] + px] = 0xFFFFFFFF;
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int[] pixels() {
        return pixels;
    }

    public boolean contains(PixelIcon icon) {
        return slots.containsKey(icon);
    }

    public float u0(PixelIcon icon) {
        return slots.get(icon)[0] / (float) width;
    }

    public float v0(PixelIcon icon) {
        return slots.get(icon)[1] / (float) height;
    }

    public float u1(PixelIcon icon) {
        return (slots.get(icon)[0] + icon.width()) / (float) width;
    }

    public float v1(PixelIcon icon) {
        return (slots.get(icon)[1] + icon.height()) / (float) height;
    }
}
