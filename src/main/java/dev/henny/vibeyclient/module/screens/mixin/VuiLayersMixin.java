package dev.henny.vibeyclient.module.screens.mixin;

import dev.henny.vibeyclient.module.screens.hook.ScreenFrame;
import dev.henny.vui.mc.VuiHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VuiHooks.class, remap = false)
abstract class VuiLayersMixin {
    @Inject(method = "afterScreen", at = @At("HEAD"), require = 0, remap = false)
    private static void vibey$unshift(Object graphics, int mx, int my, CallbackInfo ci) {
        ScreenFrame.suspend(graphics);
    }

    @Inject(method = "afterScreen", at = @At("RETURN"), require = 0, remap = false)
    private static void vibey$reshift(Object graphics, int mx, int my, CallbackInfo ci) {
        ScreenFrame.resume(graphics);
    }

    @ModifyVariable(method = "afterScreen", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0, remap = false)
    private static int vibey$mouseX(int x) {
        return (int) Math.floor(ScreenFrame.realX(x));
    }

    @ModifyVariable(method = "afterScreen", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0, remap = false)
    private static int vibey$mouseY(int y) {
        return (int) Math.floor(ScreenFrame.realY(y));
    }

    @ModifyVariable(method = {"click", "release", "scroll"}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0, remap = false)
    private static double vibey$inputX(double x) {
        return ScreenFrame.realX(x);
    }

    @ModifyVariable(method = {"click", "release", "scroll"}, at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0, remap = false)
    private static double vibey$inputY(double y) {
        return ScreenFrame.realY(y);
    }
}
