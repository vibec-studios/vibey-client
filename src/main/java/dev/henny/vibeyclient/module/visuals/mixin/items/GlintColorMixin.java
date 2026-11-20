package dev.henny.vibeyclient.module.visuals.mixin.items;

import dev.henny.vibeyclient.module.visuals.items.glint.GlintRender;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderType.class)
abstract class GlintColorMixin {
    @Shadow
    @Final
    private RenderSetup state;

    @ModifyArg(method = "draw", require = 0, index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;"
                    + "Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private Vector4fc vibey$glintColor(Vector4fc color) {
        return GlintRender.color(((RenderSetupAccess) (Object) state).vibey$textureTransform(), color);
    }
}
