package dev.henny.vibeyclient.module.waypoints.core.map;

public final class PixelOrder {
    private PixelOrder() {}

    public static void argbToAbgr(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            pixels[i] = (c & 0xFF00FF00) | ((c & 0xFF) << 16) | ((c >> 16) & 0xFF);
        }
    }
}
