package dev.henny.vibeyclient.media.player;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.nio.ByteBuffer;

public final class FrameTexture extends AbstractTexture {
    private final int w;
    private final int h;

    FrameTexture(String label, int w, int h) {
        this.w = w;
        this.h = h;
        GpuDevice device = RenderSystem.getDevice();
        texture = device.createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING, TextureFormat.RGBA8, w, h, 1, 1);
        textureView = device.createTextureView(texture);
        sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    void upload(ByteBuffer rgba) {
        rgba.position(0).limit(w * h * 4);
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, rgba, NativeImage.Format.RGBA, 0, 0, 0, 0, w, h);
    }
}
