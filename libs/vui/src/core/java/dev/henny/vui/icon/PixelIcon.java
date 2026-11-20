package dev.henny.vui.icon;

import java.util.Arrays;
import java.util.List;

public final class PixelIcon {
    private static final int ON = 0xFFFFFFFF;

    private final String name;
    private final int width;
    private final int height;
    private final int[] pixels;
    private final boolean tinted;
    private final int[] runs;

    public PixelIcon(String name, int width, int height, boolean[] cells) {
        this(name, width, height, mono(cells), true);
    }

    private PixelIcon(String name, int width, int height, int[] pixels, boolean tinted) {
        if (pixels.length != width * height) throw new IllegalArgumentException("icon " + name + ": expected " + width * height + " cells");
        this.name = name;
        this.width = width;
        this.height = height;
        this.pixels = pixels;
        this.tinted = tinted;
        this.runs = IconRuns.of(width, height, pixels);
    }

    public static PixelIcon colored(String name, int width, int height, int[] argb) {
        return new PixelIcon(name, width, height, argb.clone(), false);
    }

    public static PixelIcon fromRows(String name, List<String> rows) {
        int h = rows.size();
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        boolean[] cells = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            String r = rows.get(y);
            for (int x = 0; x < r.length(); x++) cells[y * w + x] = r.charAt(x) == '#';
        }
        return new PixelIcon(name, w, h, cells);
    }

    private static int[] mono(boolean[] cells) {
        int[] out = new int[cells.length];
        for (int i = 0; i < cells.length; i++) out[i] = cells[i] ? ON : 0;
        return out;
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean tinted() {
        return tinted;
    }

    public boolean on(int x, int y) {
        return pixel(x, y) != 0;
    }

    public int pixel(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height ? pixels[y * width + x] : 0;
    }

    public int runCount() {
        return runs.length / IconRuns.STRIDE;
    }

    public int runX(int i) {
        return runs[i * IconRuns.STRIDE];
    }

    public int runY(int i) {
        return runs[i * IconRuns.STRIDE + 1];
    }

    public int runLength(int i) {
        return runs[i * IconRuns.STRIDE + 2];
    }

    public int runColor(int i) {
        return runs[i * IconRuns.STRIDE + 3];
    }

    public String[] rows() {
        String[] out = new String[height];
        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder(width);
            for (int x = 0; x < width; x++) sb.append(on(x, y) ? '#' : '.');
            out[y] = sb.toString();
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PixelIcon p && p.width == width && p.height == height && p.tinted == tinted && Arrays.equals(p.pixels, pixels);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pixels) * 31 + width;
    }
}
