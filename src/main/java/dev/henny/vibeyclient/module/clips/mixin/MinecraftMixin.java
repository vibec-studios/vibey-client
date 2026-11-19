package dev.henny.vibeyclient.module.clips.mixin;

import dev.henny.vibeyclient.module.clips.ClipsHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture point: right after the frame was blitted to the window, where vanilla's Tracy frame capture runs. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V",
            shift = At.Shift.AFTER))
    private void vibey$clipsCapture(boolean tick, CallbackInfo ci) {
        ClipsHooks.afterBlit();
    }
}
