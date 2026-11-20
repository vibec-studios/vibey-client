package dev.henny.vibeyclient.module.screens.mixin;

import dev.henny.vibeyclient.module.screens.hook.ScreenFrame;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiSubtitlesMixin {
    @Inject(method = "renderDeferredSubtitles", at = @At("HEAD"), require = 0)
    private void vibey$unshift(CallbackInfo ci) {
        ScreenFrame.suspendCurrent();
    }

    @Inject(method = "renderDeferredSubtitles", at = @At("RETURN"), require = 0)
    private void vibey$reshift(CallbackInfo ci) {
        ScreenFrame.resumeCurrent();
    }
}
