package dev.henny.vui.icon;

import java.util.Arrays;
import java.util.List;

public final class PixelIcon {
    private final String name;
    private final int width;
    private final int height;
    private final boolean[] cells;
    private final int[] runs;

    public PixelIcon(String name, int width, int height, boolean[] cells) {
        if (cells.length != width * height) throw new IllegalArgumentException("icon " + name + ": expected " + width * height + " cells");
        this.name = name;
        this.width = width;
        this.height = height;
        this.cells = cells.clone();
        this.runs = runsOf(width, height, this.cells);
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

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean on(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height && cells[y * width + x];
    }

    public int runCount() {
        return runs.length / 3;
    }

    public int runX(int i) {
        return runs[i * 3];
    }

    public int runY(int i) {
        return runs[i * 3 + 1];
    }

    public int runLength(int i) {
        return runs[i * 3 + 2];
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

    private static int[] runsOf(int w, int h, boolean[] cells) {
        int[] out = new int[w * h * 3];
        int n = 0;
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                if (!cells[y * w + x]) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < w && cells[y * w + x]) x++;
                out[n++] = start;
                out[n++] = y;
                out[n++] = x - start;
            }
        }
        return Arrays.copyOf(out, n);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PixelIcon p && p.width == width && p.height == height && Arrays.equals(p.cells, cells);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(cells) * 31 + width;
    }
}
