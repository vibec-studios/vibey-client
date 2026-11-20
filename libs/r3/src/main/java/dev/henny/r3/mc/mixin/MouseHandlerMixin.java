package dev.henny.r3.mc.mixin;

import dev.henny.r3.mc.R3Hooks;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonInfo;
//?}

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    //? if >=1.21.9 {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void r3$button(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (R3Hooks.mouseButton(button.button(), action)) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void r3$button(long window, int button, int action, int mods, CallbackInfo ci) {
        if (R3Hooks.mouseButton(button, action)) ci.cancel();
    }
    *///?}

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void r3$scroll(long window, double dx, double dy, CallbackInfo ci) {
        if (R3Hooks.scroll(dy)) ci.cancel();
    }
}
