package dev.henny.vibeyclient.module.cinematic.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.henny.vibeyclient.module.cinematic.run.CameraDriver;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class CinematicRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float vibey$cinematicFov(float original, Camera camera, float partial, boolean useSetting) {
        return CameraDriver.fov(original, useSetting);
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void vibey$cinematicHand(float partial, boolean sleeping, Matrix4f projection, CallbackInfo ci) {
        if (CameraDriver.hideHand()) ci.cancel();
    }
}
