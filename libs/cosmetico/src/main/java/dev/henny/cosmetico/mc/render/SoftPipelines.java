package dev.henny.cosmetico.mc.render;

//? if >=1.21.11 && <26.2 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import dev.henny.cosmetico.mc.compat.McIds;

final class SoftPipelines {
    private SoftPipelines() {}

    private static RenderPipeline translucent, glow;

    private static RenderPipeline.Builder base(String name) {
        return RenderPipeline.builder()
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("Fog", UniformType.UNIFORM_BUFFER)
                .withLocation(McIds.id("pipeline/" + name))
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
                .withShaderDefine("ALPHA_CUTOUT", 0.004f);
    }

    static RenderPipeline translucent() {
        if (translucent == null) {
            translucent = base("cosmetic_translucent")
                    .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler1")
                    .withSampler("Sampler2")
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .build();
        }
        return translucent;
    }

    static RenderPipeline glow() {
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
//?} else {
/*final class SoftPipelines {
    private SoftPipelines() {}
}
*///?}
