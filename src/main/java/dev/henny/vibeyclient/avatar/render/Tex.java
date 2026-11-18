package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.henny.vibeyclient.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * A texture uploaded by the engine plus its render types. Hides the per-version id type (Identifier /
 * ResourceLocation), the DynamicTexture constructor and the RenderType factory location. Render thread only.
 */
public final class Tex {
    private static int counter;

    //? if >=1.21.11 {
    public final Identifier id;
    //?} else
    /*public final ResourceLocation id;*/
    public final int width, height;
    private DynamicTexture texture;
    private RenderType cutout, translucent, emissive;

    private Tex(String prefix, NativeImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        //? if >=1.21.5 {
        this.texture = new DynamicTexture(() -> "vibeyclient " + prefix, image);
        //?} else
        /*this.texture = new DynamicTexture(image);*/
        this.id = Compat.id("dyn/" + prefix + "_" + (++counter));
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    /** Decodes a PNG and uploads it (render thread). */
    public static Tex upload(String prefix, byte[] png) throws java.io.IOException {
        NativeImage img = NativeImage.read(png);
        return new Tex(sanitize(prefix), img);
    }

    public static Tex of(String prefix, NativeImage img) {
        return new Tex(sanitize(prefix), img);
    }

    private static String sanitize(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && b.length() < 48; i++) {
            char c = Character.toLowerCase(s.charAt(i));
            b.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' ? c : '_');
        }
        return b.toString();
    }

    public DynamicTexture texture() {
        return texture;
    }

    public NativeImage pixels() {
        return texture.getPixels();
    }

    /** Re-uploads after {@link #pixels()} changed. */
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

    // -------------------------------------------------------------------------------------------- render types

    /** Opaque/cutout entity geometry, no back face culling (planes are common in cosmetics). */
    public RenderType cutout() {
        if (cutout == null) {
            //? if >=26.1 {
            /*cutout = RenderTypes.entityCutout(id);
            *///?} else if >=1.21.11 {
            cutout = RenderTypes.entityCutoutNoCull(id);
            //?} else
            /*cutout = RenderType.entityCutoutNoCull(id);*/
        }
        return cutout;
    }

    public RenderType translucent() {
        if (translucent == null) {
            //? if >=1.21.11 {
            translucent = RenderTypes.entityTranslucent(id);
            //?} else
            /*translucent = RenderType.entityTranslucent(id);*/
        }
        return translucent;
    }

    /** Glowing: no shading from the world light, alpha blended. */
    public RenderType emissive() {
        if (emissive == null) {
            //? if >=1.21.11 {
            emissive = RenderTypes.entityTranslucentEmissive(id);
            //?} else
            /*emissive = RenderType.entityTranslucentEmissive(id);*/
            //? if =1.21.5
            /*EMISSIVE.add(emissive);*/
        }
        return emissive;
    }

    //? if =1.21.5 {
    /*private static final java.util.Set<RenderType> EMISSIVE = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    /^* 1.21.5 only: {@code type} is one of our emissive render types (see MeshSink.Buffers). ^/
    static boolean isEmissive(RenderType type) {
        return EMISSIVE.contains(type);
    }
    *///?}
}
