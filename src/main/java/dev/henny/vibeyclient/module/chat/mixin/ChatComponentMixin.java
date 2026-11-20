package dev.henny.vibeyclient.module.chat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.vibeyclient.module.chat.hook.ChatHooks;
import dev.henny.vibeyclient.module.chat.hook.Incoming;
import dev.henny.vibeyclient.module.chat.window.ChatWindow;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    public abstract void addMessage(Component component, MessageSignature signature, GuiMessageTag tag);

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void vibey$incoming(Component component, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        if (ChatHooks.reentering()) return;
        Incoming in = ChatHooks.incoming((ChatComponent) (Object) this, component, tag);
        if (in.drop()) {
            ci.cancel();
        } else if (in.tag() != tag) {
            ci.cancel();
            ChatHooks.reenter(() -> addMessage(component, signature, in.tag()));
        }
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
    private void vibey$display(GuiMessage message, CallbackInfo ci) {
        if (!ChatHooks.display(message)) ci.cancel();
    }

    @WrapOperation(method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    private List<FormattedCharSequence> vibey$split(GuiMessage message, Font font, int width, Operation<List<FormattedCharSequence>> op) {
        return op.call(ChatHooks.decorated(message), font, width);
    }

    @ModifyArg(method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V"))
    private Object vibey$line(Object line) {
        ChatHooks.line((GuiMessage.Line) line);
        return line;
    }

    @ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
    private int vibey$history(int vanilla) {
        return ChatHooks.historySize(vanilla);
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;updatePose(Ljava/util/function/Consumer;)V"))
    private Consumer<Matrix3x2f> vibey$move(Consumer<Matrix3x2f> vanilla) {
        return ChatWindow.wrap(vanilla);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V", at = @At("HEAD"), cancellable = true)
    private void vibey$hidden(GuiGraphics g, Font font, int ticks, int mouseX, int mouseY, boolean focused, boolean insertion, CallbackInfo ci) {
        if (!focused && ChatWindow.hideUnfocused()) ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vibey$tick(CallbackInfo ci) {
        ChatHooks.tick((ChatComponent) (Object) this);
    }
}
