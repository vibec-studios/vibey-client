package dev.henny.r3.mc.mixin;

import dev.henny.r3.mc.R3Hooks;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.9 <26.2 {
import net.minecraft.client.Camera;
//?}
//? if >=1.21.9 <26.1 {
import net.minecraft.client.renderer.state.LevelRenderState;
//?}
//? if >=26.1 <26.2 {
/*import net.minecraft.client.renderer.state.level.LevelRenderState;
*///?}

@Mixin(LevelRenderer.class)
abstract class BlockOutlineMixin {
    //? if >=1.21.9 <26.2 {
    @Inject(method = "extractBlockOutline", at = @At("HEAD"), cancellable = true)
    private void r3$hideOutline(Camera camera, LevelRenderState state, CallbackInfo ci) {
        if (!R3Hooks.hidesBlockOutline()) return;
        state.blockOutlineRenderState = null;
        ci.cancel();
    }
    //?}
    //? if >=26.2 {
    /*@Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void r3$hideOutline(CallbackInfo ci) {
        if (R3Hooks.hidesBlockOutline()) ci.cancel();
    }
    *///?}
    //? if <1.21.9 {
    /*@Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void r3$hideOutline(CallbackInfo ci) {
        if (R3Hooks.hidesBlockOutline()) ci.cancel();
    }
    *///?}
}
