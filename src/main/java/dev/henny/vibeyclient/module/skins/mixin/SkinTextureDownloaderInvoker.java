package dev.henny.vibeyclient.module.skins.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SkinTextureDownloader.class)
public interface SkinTextureDownloaderInvoker {
    @Invoker("processLegacySkin")
    static NativeImage vibey$processLegacySkin(NativeImage image, String url) {
        throw new AssertionError();
    }
}
