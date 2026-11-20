package dev.henny.vibeyclient.module.screens.mixin;

import dev.henny.vibeyclient.module.screens.hook.ScreenStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
abstract class TitleTextMixin {
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vibey$hideTitle(Font font, Component c, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (ScreenStyle.hideText(c)) ci.cancel();
    }

    @Inject(method = "drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void vibey$hideCenteredTitle(Font font, Component c, int x, int y, int color, CallbackInfo ci) {
        if (ScreenStyle.hideText(c)) ci.cancel();
    }
}
