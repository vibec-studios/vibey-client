package dev.henny.vibeyclient.module.chat.notify;

import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.DirectMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatNotifier {
    private static final long MENTION_GAP_MS = 1500;
    private static long lastMention;

    private ChatNotifier() {}

    public static void notify(ChatModule m, DirectMessage dm, boolean mention, String plain) {
        if (dm == null && !mention) return;
        long now = System.currentTimeMillis();
        if (dm == null) {
            if (now - lastMention < MENTION_GAP_MS) return;
            lastMention = now;
        }
        if (m.mentionSound.on()) ChatSound.ping();
        if (Minecraft.getInstance().screen instanceof ChatScreen) return;
        if (dm != null && m.dmToast.on()) ChatToasts.dm(dm);
        else if (mention && m.mentionToast.on()) ChatToasts.mention(plain);
    }
}
