package dev.henny.cosmetico.mc.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.cosmetico.mc.compat.McIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}

public final class Tex {
    private static int counter;

    //? if >=1.21.11 {
    public final Identifier id;
    //?} else
    /*public final ResourceLocation id;*/
    public final int width, height;
    private DynamicTexture texture;
    RenderType cutout, translucent, emissive, glow;

    private Tex(String prefix, NativeImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        //? if >=1.21.5 {
        this.texture = new DynamicTexture(() -> "cosmetico " + prefix, image);
        //?} else
        /*this.texture = new DynamicTexture(image);*/
        this.id = McIds.id("dyn/" + prefix + "_" + (++counter));
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    public static Tex upload(String prefix, byte[] png) throws java.io.IOException {
        return new Tex(TexNames.sanitize(prefix), NativeImage.read(png));
    }

    public static Tex of(String prefix, NativeImage img) {
        return new Tex(TexNames.sanitize(prefix), img);
    }

    public NativeImage pixels() {
        return texture.getPixels();
    }

    public void upload() {
        texture.upload();
    }

    public void release() {
        if (texture == null) return;
        Minecraft.getInstance().getTextureManager().release(id);
        texture = null;
    }

    public boolean released() {
        return texture == null;
    }

    public RenderType cutout() {
        return EntityRenderTypes.cutout(this);
    }

    public RenderType translucent() {
        return EntityRenderTypes.translucent(this);
    }

    public RenderType emissive() {
        return EntityRenderTypes.emissive(this);
    }

    public RenderType glow() {
        return EntityRenderTypes.glow(this);
    }
}
