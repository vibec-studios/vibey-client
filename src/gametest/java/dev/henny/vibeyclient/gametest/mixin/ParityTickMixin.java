package dev.henny.vibeyclient.gametest.mixin;

import dev.henny.vibeyclient.gametest.parity.ParityClock;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class ParityTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void vibeyGametest$tick(CallbackInfo ci) {
        ParityClock.onTick();
    }
}
