package dev.henny.vibeyclient.module.inventory.mixin;

import dev.henny.vibeyclient.module.inventory.presets.PresetKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseScrollMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void vibey$presetScroll(long window, double dx, double dy, CallbackInfo ci) {
        if (window != minecraft.getWindow().handle() || minecraft.screen != null || minecraft.player == null) return;
        if (PresetKeys.scroll(dy)) ci.cancel();
    }
}
