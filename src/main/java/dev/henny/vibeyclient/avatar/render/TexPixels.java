package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.platform.NativeImage;

/** NativeImage pixel access across versions (1.21.1 only has the RGBA/ABGR accessors). Byte order is irrelevant here. */
final class TexPixels {
    private TexPixels() {}

    static int get(NativeImage img, int x, int y) {
        //? if >=1.21.3 {
        return img.getPixel(x, y);
        //?} else
        /*return img.getPixelRGBA(x, y);*/
    }

    static void set(NativeImage img, int x, int y, int c) {
        //? if >=1.21.3 {
        img.setPixel(x, y, c);
        //?} else
        /*img.setPixelRGBA(x, y, c);*/
    }

    /** Per-channel blend (works for ARGB and ABGR alike). */
    static int lerp(int a, int b, float t) {
        int out = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = (a >>> s) & 255, cb = (b >>> s) & 255;
            out |= (Math.round(ca + (cb - ca) * t) & 255) << s;
        }
        return out;
    }
}
