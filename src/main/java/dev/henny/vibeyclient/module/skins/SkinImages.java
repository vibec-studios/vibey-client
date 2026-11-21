package dev.henny.vibeyclient.module.skins;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.skins.mixin.SkinTextureDownloaderInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.concurrent.atomic.AtomicInteger;

public final class SkinImages {
    private static final AtomicInteger IDS = new AtomicInteger();

    private SkinImages() {}

    public static Identifier upload(String kind, byte[] png) {
        if (png == null) return null;
        try {
            NativeImage img = NativeImage.read(png);
            NativeImage fixed = SkinTextureDownloaderInvoker.vibey$processLegacySkin(img, "vibey:" + kind);
            if (fixed == null) return null;
            DynamicTexture tex = new DynamicTexture(() -> "vibeyclient skin " + kind, fixed);
            Identifier id = Identifier.fromNamespaceAndPath("vibeyclient", "skins/" + kind + "_" + IDS.incrementAndGet());
            Minecraft.getInstance().getTextureManager().register(id, tex);
            return id;
        } catch (Exception e) {
            Vibey.LOG.debug("Skins: image unreadable: {}", e.toString());
            return null;
        }
    }

    public static void release(Identifier id) {
        if (id != null) Minecraft.getInstance().getTextureManager().release(id);
    }
}
