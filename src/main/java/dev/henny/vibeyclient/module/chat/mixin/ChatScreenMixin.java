package dev.henny.vibeyclient.module.chat.mixin;

import dev.henny.vibeyclient.module.chat.screen.ChatKeys;
import dev.henny.vibeyclient.module.chat.screen.CopyButton;
import dev.henny.vibeyclient.module.chat.screen.SearchMode;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow
    protected EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void vibey$init(CallbackInfo ci) {
        SearchMode.initialized(input);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void vibey$key(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ChatKeys.key(input, event.key(), event.modifiers())) cir.setReturnValue(true);
    }

    @Inject(method = "onEdited", at = @At("HEAD"), cancellable = true)
    private void vibey$edited(String text, CallbackInfo ci) {
        if (!SearchMode.active()) return;
        SearchMode.edited(text);
        ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vibey$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (CopyButton.click(event.x(), event.y(), event.button())) cir.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void vibey$render(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SearchMode.frame();
        CopyButton.draw(new Gfx(graphics, Minecraft.getInstance().font), mouseX, mouseY);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void vibey$removed(CallbackInfo ci) {
        SearchMode.exit();
    }
}
