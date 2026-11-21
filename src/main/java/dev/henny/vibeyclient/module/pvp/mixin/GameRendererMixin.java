package dev.henny.vibeyclient.module.pvp.mixin;

import dev.henny.vibeyclient.module.pvp.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zoom: divides the world FOV (not the hand's) by the current zoom factor. Up to 1.21.11 that is
 * {@code GameRenderer.getFov} with {@code useFovSetting = true}; from 26.1 the camera computes it in
 * {@code calculateFov}.
 */
//? if >=26.1 {
/*@Mixin(Camera.class)
*///?} else
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
abstract class GameRendererMixin {
    //? if >=26.1 {
    /*@Inject(method = "calculateFov", require = 0, at = @At("RETURN"), cancellable = true)
    private void vibey$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        double f = Zoom.factor();
        if (f != 1) cir.setReturnValue((float) (cir.getReturnValueF() / f));
    }
    *///?} else if >=1.21.2 {
    @Inject(method = "getFov", require = 0, at = @At("RETURN"), cancellable = true)
    private void vibey$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting) return;
        double f = Zoom.factor();
        if (f != 1) cir.setReturnValue((float) (cir.getReturnValueF() / f));
    }
    //?} else {
    /*@Inject(method = "getFov", require = 0, at = @At("RETURN"), cancellable = true)
    private void vibey$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (!useFovSetting) return;
        double f = Zoom.factor();
        if (f != 1) cir.setReturnValue(cir.getReturnValueD() / f);
    }
    *///?}
}
