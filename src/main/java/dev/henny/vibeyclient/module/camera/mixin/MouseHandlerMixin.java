package dev.henny.vibeyclient.module.camera.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.GameCompat;
import dev.henny.vibeyclient.module.camera.look.CameraState;
import dev.henny.vibeyclient.module.camera.look.Orbit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "handleAccumulatedMovement", require = 0, at = @At("HEAD"))
    private void vibey$cameraFrame(CallbackInfo ci) {
        CameraState.frame(minecraft);
    }

    @WrapOperation(method = "turnPlayer", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void vibey$cameraTurn(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        if (!CameraState.turn(minecraft, dx, dy)) original.call(player, dx, dy);
    }

    //? if >=1.21.9 {
    @Inject(method = "onButton", require = 0, at = @At("HEAD"), cancellable = true)
    private void vibey$orbitButton(long window, net.minecraft.client.input.MouseButtonInfo button, int action, CallbackInfo ci) {
        vibey$handleOrbit(window, dev.henny.vibeyclient.client.Keys.mouseFromNative(button.button()), action, ci);
    }
    //?} else {
    /*@Inject(method = "onPress", require = 0, at = @At("HEAD"), cancellable = true)
    private void vibey$orbitButton(long window, int button, int action, int mods, CallbackInfo ci) {
        vibey$handleOrbit(window, button, action, ci);
    }
    *///?}

    private void vibey$handleOrbit(long window, int button, int action, CallbackInfo ci) {
        Minecraft mc = minecraft;
        if (button != Orbit.MIDDLE || window != GameCompat.windowHandle()) return;
        if (action == 0) {
            if (Orbit.release(mc)) ci.cancel();
        } else if (action == 1 && ClientCompat.screen() == null && !ClientCompat.loadingOverlay() && Orbit.press(mc)) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", require = 0, at = @At("HEAD"), cancellable = true)
    private void vibey$orbitZoom(long window, double dx, double dy, CallbackInfo ci) {
        if (window == GameCompat.windowHandle() && ClientCompat.screen() == null && Orbit.wheel(dy)) ci.cancel();
    }
}
