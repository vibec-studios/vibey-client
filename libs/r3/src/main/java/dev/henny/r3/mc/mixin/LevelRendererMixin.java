package dev.henny.r3.mc.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.9 {
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.r3.mc.R3Hooks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
//? if >=26.1 {
/*import net.minecraft.client.renderer.state.level.LevelRenderState;
*///?}
//? if >=1.21.9 <26.1 {
import net.minecraft.client.renderer.state.LevelRenderState;
//?}

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    //? if >=1.21.9 {
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void r3$submitWorld(PoseStack pose, LevelRenderState state, SubmitNodeCollector out, CallbackInfo ci) {
        R3Hooks.submitWorld(pose, out);
    }
    //?}
}
