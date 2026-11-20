package dev.henny.r3.mc.texture;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.r3.render.TextureHandle;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.Locale;

public final class DynamicTextureHandle implements TextureHandle, McTexture {
    private final String id;
    private final int width;
    private final int height;
    private DynamicTexture texture;

    public DynamicTextureHandle(String name, int width, int height) {
        this.id = "r3:dynamic/" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        this.width = width;
        this.height = height;
    }

    @Override
    public String textureId() {
        return id;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    public void upload(int[] argb) {
        ensure();
        NativeImage image = texture.getPixels();
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) NativeTextures.setPixel(image, x, y, argb[row + x]);
        }
        texture.upload();
    }

    public void close() {
        if (texture == null) return;
        NativeTextures.release(id);
        texture = null;
    }

    private void ensure() {
        if (texture != null) return;
        texture = NativeTextures.create(id, width, height);
        NativeTextures.register(id, texture);
    }
}
