package dev.henny.r3.mc.texture;

import net.minecraft.client.renderer.texture.DynamicTexture;

public final class WhiteTexture {
    public static final String ID = "r3:white";
    private static boolean registered;

    private WhiteTexture() {}

    public static void ensure() {
        if (registered) return;
        DynamicTexture texture = NativeTextures.create(ID, 4, 4);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) NativeTextures.setPixel(texture.getPixels(), x, y, 0xFFFFFFFF);
        }
        texture.upload();
        NativeTextures.register(ID, texture);
        registered = true;
    }
}
