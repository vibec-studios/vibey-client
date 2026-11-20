package dev.henny.vibeyclient.module.chat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.vibeyclient.module.chat.ChatModule;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @WrapOperation(method = "onDisconnected",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"))
    private void vibey$keepChat(ChatComponent chat, boolean history, Operation<Void> op) {
        ChatModule m = ChatModule.get();
        if (m != null && m.isEnabled() && m.keepOnLeave.on()) return;
        op.call(chat, history);
    }
}
