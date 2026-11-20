package dev.henny.vibeyclient.media.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class ExternalTexture extends AbstractTexture {
    public ExternalTexture() {
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
