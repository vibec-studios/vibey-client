package dev.henny.vui.icon;

import java.util.Arrays;

final class IconRuns {
    static final int STRIDE = 4;

    private IconRuns() {
    }

    static int[] of(int w, int h, int[] pixels) {
        int[] out = new int[w * h * STRIDE];
        int n = 0;
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                int color = pixels[y * w + x];
                if (color == 0) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < w && pixels[y * w + x] == color) x++;
                out[n++] = start;
                out[n++] = y;
                out[n++] = x - start;
                out[n++] = color;
            }
        }
        return Arrays.copyOf(out, n);
    }
}
