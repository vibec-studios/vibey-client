package dev.henny.vibeyclient.gametest.mixin;

import dev.henny.vibeyclient.gametest.parity.ParityClock;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
abstract class ParityPartialMixin {
    @Inject(method = "getGameTimeDeltaPartialTick", at = @At("HEAD"), cancellable = true)
    private void vibeyGametest$partial(boolean ignoreFrozen, CallbackInfoReturnable<Float> cir) {
        if (!Double.isNaN(ParityClock.now())) cir.setReturnValue(1f);
    }
}
