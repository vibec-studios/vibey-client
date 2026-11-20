package dev.henny.vibeyclient.module.cinematic.mixin;

import dev.henny.vibeyclient.module.cinematic.run.InputGate;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class CinematicInputMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void vibey$afkButton(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (InputGate.button(action)) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void vibey$afkScroll(long window, double dx, double dy, CallbackInfo ci) {
        if (InputGate.scroll()) ci.cancel();
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void vibey$afkMove(long window, double x, double y, CallbackInfo ci) {
        InputGate.move(x, y);
    }
}
