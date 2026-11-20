package dev.henny.vibeyclient.module.nametags.mixin;

import com.mojang.authlib.GameProfile;
import dev.henny.vibeyclient.client.names.Profiles;
import dev.henny.vibeyclient.module.nametags.NameIcons;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    @ModifyVariable(method = "handlePlayerChatMessage", at = @At("HEAD"), argsOnly = true)
    private ChatType.Bound vibey$senderName(ChatType.Bound bound, PlayerChatMessage message, GameProfile sender, ChatType.Bound same) {
        if (bound == null || sender == null) return bound;
        Component name = bound.name();
        Component out = NameIcons.chat(Profiles.id(sender), Profiles.name(sender), name);
        return out == name ? bound : new ChatType.Bound(bound.chatType(), out, bound.targetName());
    }
}
