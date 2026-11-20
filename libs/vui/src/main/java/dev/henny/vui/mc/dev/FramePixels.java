package dev.henny.vui.mc.dev;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vui.mc.compat.Mc;
import net.minecraft.client.Screenshot;

import java.util.function.Consumer;

record FramePixels(int width, int height, int[] argb) {
    static void grab(Consumer<FramePixels> done) {
        //? if >=1.21.5 {
        Screenshot.takeScreenshot(Mc.mainRenderTarget(), image -> deliver(image, done));
        //?} else
        /*deliver(Screenshot.takeScreenshot(Mc.mainRenderTarget()), done);*/
    }

    private static void deliver(NativeImage image, Consumer<FramePixels> done) {
        try {
            done.accept(copy(image));
        } finally {
            image.close();
        }
    }

    private static FramePixels copy(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        //? if >=1.21.2 {
        int[] argb = image.getPixels();
        //?} else {
        /*int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int abgr = image.getPixelRGBA(x, y);
                argb[y * w + x] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
            }
        }
        *///?}
        return new FramePixels(w, h, argb);
    }
}
