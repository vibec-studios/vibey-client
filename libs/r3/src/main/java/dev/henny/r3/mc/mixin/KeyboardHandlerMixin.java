package dev.henny.r3.mc.mixin;

import dev.henny.r3.mc.R3Hooks;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
//?}

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
    //? if >=1.21.9 {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void r3$key(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (R3Hooks.key(event.key(), action)) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void r3$key(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (R3Hooks.key(key, action)) ci.cancel();
    }
    *///?}
}
