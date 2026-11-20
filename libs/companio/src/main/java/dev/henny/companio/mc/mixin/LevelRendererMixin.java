package dev.henny.companio.mc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.companio.mc.render.CompanionRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void companio$submit(PoseStack pose, LevelRenderState state, SubmitNodeCollector out, CallbackInfo ci) {
        CompanionRenderer.submit(pose, state, out);
    }
}
