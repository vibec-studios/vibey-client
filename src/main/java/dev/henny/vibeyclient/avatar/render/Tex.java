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
    private RenderType cutout, translucent, emissive, glow;

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

    /**
     * Lit, alpha blended. 1.21.11 and 26.1: our own pipeline with an alpha cutout of 0.004 instead of vanilla's 0.1, so
     * soft alpha gradients fade out smoothly instead of ending in a hard edge (26.2+: vanilla, new pipeline API).
     */
    public RenderType translucent() {
        if (translucent == null) {
            //? if >=1.21.11 && <26.2 {
            translucent = dev.henny.vibeyclient.mixin.client.RenderTypeInvoker.vibey$create("vibeyclient_cosmetic_translucent",
                    net.minecraft.client.renderer.rendertype.RenderSetup.builder(SoftPipelines.translucent())
                            .withTexture("Sampler0", id).useLightmap().useOverlay().affectsCrumbling().sortOnUpload()
                            .setOutline(net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                            .createRenderSetup());
            //?} else if >=1.21.11 {
            /*translucent = RenderTypes.entityTranslucent(id);
            *///?} else
            /*translucent = RenderType.entityTranslucent(id);*/
        }
        return translucent;
    }

    /**
     * Emissive + translucent bones (aurora veils, glows): full-bright, no per-face shading, alpha blended without
     * depth writes and without vanilla's 0.1 alpha cutout (1.21.11 and 26.1). Other versions use {@link #emissive()}.
     */
    public RenderType glow() {
        if (glow == null) {
            //? if >=1.21.11 && <26.2 {
            glow = dev.henny.vibeyclient.mixin.client.RenderTypeInvoker.vibey$create("vibeyclient_cosmetic_glow",
                    net.minecraft.client.renderer.rendertype.RenderSetup.builder(SoftPipelines.glow())
                            .withTexture("Sampler0", id).sortOnUpload().createRenderSetup());
            //?} else
            /*glow = emissive();*/
        }
        return glow;
    }

    //? if >=1.21.11 && <26.2 {
    /** The engine's soft-alpha pipelines (built lazily, compiled by the device on first use). */
    static final class SoftPipelines {
        private static com.mojang.blaze3d.pipeline.RenderPipeline translucent, glow;

        private static com.mojang.blaze3d.pipeline.RenderPipeline.Builder base(String name) {
            return com.mojang.blaze3d.pipeline.RenderPipeline.builder()
                    .withUniform("DynamicTransforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
                    .withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
                    .withUniform("Fog", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
                    .withLocation(Compat.id("pipeline/" + name))
                    .withVertexShader("core/entity")
                    .withFragmentShader("core/entity")
                    .withSampler("Sampler0")
                    //? if >=26.1 {
                    /*.withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.ENTITY, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
                    .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(com.mojang.blaze3d.pipeline.DepthStencilState.DEFAULT)
                    *///?} else {
                    .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
                    .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
                    //?}
                    .withCull(false)
                    // only fully transparent texels are dropped (they must not write depth)
                    .withShaderDefine("ALPHA_CUTOUT", 0.004f);
        }

        static com.mojang.blaze3d.pipeline.RenderPipeline translucent() {
            if (translucent == null) {
                translucent = base("cosmetic_translucent")
                        .withUniform("Lighting", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
                        .withSampler("Sampler1")
                        .withSampler("Sampler2")
                        .withShaderDefine("PER_FACE_LIGHTING")
                        .build();
            }
            return translucent;
        }

        static com.mojang.blaze3d.pipeline.RenderPipeline glow() {
            if (glow == null) {
                glow = base("cosmetic_glow")
                        .withShaderDefine("EMISSIVE")
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        //? if >=26.1 {
                        /*.withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL, false))
                        *///?} else
                        .withDepthWrite(false)
                        .build();
            }
            return glow;
        }
    }
    //?}

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
