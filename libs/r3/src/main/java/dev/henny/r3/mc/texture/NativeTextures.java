package dev.henny.r3.mc.texture;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.mc.compat.Ids;
import net.minecraft.client.renderer.texture.DynamicTexture;

final class NativeTextures {
    private NativeTextures() {}

    static DynamicTexture create(String label, int width, int height) {
        //? if >=1.21.5 {
        return new DynamicTexture(() -> label, width, height, true);
        //?} else
        /*return new DynamicTexture(width, height, true);*/
    }

    static void setPixel(NativeImage image, int x, int y, int argb) {
        //? if >=1.21.5 {
        image.setPixel(x, y, argb);
        //?} else
        /*image.setPixelRGBA(x, y, (argb & 0xFF00FF00) | (argb >> 16 & 0xFF) | (argb & 0xFF) << 16);*/
    }

    static void register(String id, DynamicTexture texture) {
        Game.mc().getTextureManager().register(Ids.parse(id), texture);
    }

    static void release(String id) {
        Game.mc().getTextureManager().release(Ids.parse(id));
    }
}
