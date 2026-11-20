package dev.henny.vibeyclient.gametest.mixin;

import dev.henny.vibeyclient.avatar.anim.Clock;
import dev.henny.vibeyclient.gametest.parity.ParityClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Clock.class, remap = false)
abstract class OldClockMixin {
    @Inject(method = "now", at = @At("HEAD"), cancellable = true)
    private static void vibeyGametest$pinned(CallbackInfoReturnable<Double> cir) {
        double t = ParityClock.now();
        if (!Double.isNaN(t)) cir.setReturnValue(t);
    }
}
