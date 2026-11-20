package dev.henny.vibeyclient.module.chat.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("allMessages")
    List<GuiMessage> vibey$allMessages();

    @Accessor("trimmedMessages")
    List<GuiMessage.Line> vibey$trimmedMessages();

    @Accessor("chatScrollbarPos")
    int vibey$scrollPos();

    @Invoker("refreshTrimmedMessages")
    void vibey$refresh();
}
