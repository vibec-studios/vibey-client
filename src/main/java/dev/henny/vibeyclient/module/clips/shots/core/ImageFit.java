package dev.henny.vibeyclient.module.clips.shots.core;

public final class ImageFit {
    private ImageFit() {}

    public static int[] coverSource(int srcW, int srcH, int dstW, int dstH) {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return new int[] {0, 0, Math.max(0, srcW), Math.max(0, srcH)};
        double src = (double) srcW / srcH, dst = (double) dstW / dstH;
        if (src > dst) {
            int w = (int) Math.round(srcH * dst);
            return new int[] {(srcW - w) / 2, 0, w, srcH};
        }
        int h = (int) Math.round(srcW / dst);
        return new int[] {0, (srcH - h) / 2, srcW, h};
    }

    public static int[] containTarget(int srcW, int srcH, int dstW, int dstH) {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return new int[] {0, 0, Math.max(0, dstW), Math.max(0, dstH)};
        double scale = Math.min((double) dstW / srcW, (double) dstH / srcH);
        int w = Math.max(1, (int) Math.round(srcW * scale)), h = Math.max(1, (int) Math.round(srcH * scale));
        return new int[] {(dstW - w) / 2, (dstH - h) / 2, w, h};
    }
}
