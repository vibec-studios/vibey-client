package dev.henny.vibeyclient.gametest.mixin;

import dev.henny.vibeyclient.gametest.FrameClock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class FrameClockMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void vibeyTest$frame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        FrameClock.sample();
    }
}
