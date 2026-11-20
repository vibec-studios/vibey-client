package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.platform.NativeImage;

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
}
