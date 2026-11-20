package dev.henny.vibeyclient.module.media.video.play;

public final class PixelSize {
    private static final int MAX = 2560;

    private PixelSize() {}

    public static int[] of(int lines, float aspect, boolean web) {
        float a = Math.max(0.3f, Math.min(4f, aspect));
        int w = Math.round(lines * a);
        int h = lines;
        if (a < 1f) {
            w = lines;
            h = Math.round(lines / a);
        }
        if (web) {
            float k = Math.max(1f, Math.max(480f / w, 270f / h));
            w = Math.round(w * k);
            h = Math.round(h * k);
        }
        return new int[] {Math.min(MAX, w) & ~1, Math.min(MAX, h) & ~1};
    }
}
