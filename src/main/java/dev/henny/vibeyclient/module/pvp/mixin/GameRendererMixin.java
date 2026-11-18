package dev.henny.vibeyclient.module.pvp.mixin;

import dev.henny.vibeyclient.module.pvp.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: divides the world FOV (not the hand's, {@code useFovSetting = false}) by the current zoom factor. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void vibey$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting) return;
        double f = Zoom.factor();
        if (f != 1) cir.setReturnValue((float) (cir.getReturnValueF() / f));
    }
}
