package dev.henny.vibeyclient.module.media.video.player;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.nio.ByteBuffer;

/** A GPU texture we write raw RGBA frames into, straight from a direct buffer (no NativeImage copy). */
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

    /** Render thread, outside a render pass. {@code rgba} holds exactly one w*h*4 frame from position 0. */
    void upload(ByteBuffer rgba) {
        rgba.position(0).limit(w * h * 4);
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, rgba, NativeImage.Format.RGBA, 0, 0, 0, 0, w, h);
    }

    /** A texture whose GPU objects belong to someone else (the browser): we only point at them. */
    public static final class External extends AbstractTexture {
        public External() {
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }

        public void set(GpuTexture t, GpuTextureView v) {
            texture = t;
            textureView = v;
        }

        public boolean ready() {
            return texture != null && textureView != null && !texture.isClosed() && !textureView.isClosed();
        }

        @Override
        public void close() {
            texture = null;
            textureView = null;
        }
    }
}
