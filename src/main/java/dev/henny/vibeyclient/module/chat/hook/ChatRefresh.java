package dev.henny.vibeyclient.module.chat.hook;

import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.module.chat.mixin.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;

public final class ChatRefresh {
    private static volatile boolean pending;
    private static int privacyVersion = -1;

    private ChatRefresh() {}

    public static void later() {
        pending = true;
    }

    public static void tick(ChatComponent chat) {
        int v = Privacy.version();
        if (v != privacyVersion) {
            privacyVersion = v;
            pending = true;
        }
        if (!pending) return;
        pending = false;
        ((ChatComponentAccessor) chat).vibey$refresh();
    }

    public static void now() {
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        chat.resetChatScroll();
        ((ChatComponentAccessor) chat).vibey$refresh();
    }
}
