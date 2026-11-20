package dev.henny.vibeyclient.gametest.mixin;

import dev.henny.vibeyclient.gametest.parity.ParityClock;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class ParityFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void vibeyGametest$frame(CallbackInfo ci) {
        ParityClock.pin();
    }
}
