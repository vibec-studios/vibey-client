package dev.henny.vibeyclient.module.clips.shots.core;

public final class Downscale {
    public static final int BACKGROUND = 0xFF0B0B0F;

    private Downscale() {}

    public static int[] cover(int[] src, int sw, int sh, int dw, int dh) {
        int[] r = ImageFit.coverSource(sw, sh, dw, dh);
        return box(src, sw, r[0], r[1], r[2], r[3], dw, dh);
    }

    public static int[] contain(int[] src, int sw, int sh, int dw, int dh) {
        int[] t = ImageFit.containTarget(sw, sh, dw, dh);
        int[] inner = box(src, sw, 0, 0, sw, sh, t[2], t[3]);
        int[] out = new int[dw * dh];
        java.util.Arrays.fill(out, BACKGROUND);
        for (int y = 0; y < t[3]; y++) System.arraycopy(inner, y * t[2], out, (y + t[1]) * dw + t[0], t[2]);
        return out;
    }

    public static int[] box(int[] src, int stride, int rx, int ry, int rw, int rh, int dw, int dh) {
        int[] out = new int[dw * dh];
        for (int y = 0; y < dh; y++) {
            int y0 = ry + (int) ((long) y * rh / dh), y1 = Math.max(y0 + 1, ry + (int) ((long) (y + 1) * rh / dh));
            for (int x = 0; x < dw; x++) {
                int x0 = rx + (int) ((long) x * rw / dw), x1 = Math.max(x0 + 1, rx + (int) ((long) (x + 1) * rw / dw));
                out[y * dw + x] = average(src, stride, x0, y0, x1, y1);
            }
        }
        return out;
    }

    private static int average(int[] src, int stride, int x0, int y0, int x1, int y1) {
        long r = 0, g = 0, b = 0;
        int n = 0;
        for (int y = y0; y < y1; y++) {
            int row = y * stride;
            for (int x = x0; x < x1; x++) {
                int p = src[row + x];
                r += (p >> 16) & 0xFF;
                g += (p >> 8) & 0xFF;
                b += p & 0xFF;
                n++;
            }
        }
        if (n == 0) return BACKGROUND;
        return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }
}
