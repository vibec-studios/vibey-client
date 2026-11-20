package dev.henny.vibeyclient.module.chat.hook;

import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.ChatEntry;
import dev.henny.vibeyclient.module.chat.core.HistorySize;
import dev.henny.vibeyclient.module.chat.core.SearchQuery;
import dev.henny.vibeyclient.module.chat.line.LineDecor;
import dev.henny.vibeyclient.module.chat.mixin.ChatComponentAccessor;
import dev.henny.vibeyclient.module.chat.screen.SearchState;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

public final class ChatHooks {
    public static final ChatIndex INDEX = new ChatIndex();
    private static final int PRUNE_EVERY = 100;

    private static boolean reentering;
    private static ChatEntry pending;
    private static GuiMessage current;
    private static int ticks;

    private ChatHooks() {}

    public static boolean reentering() {
        return reentering;
    }

    public static void reenter(Runnable r) {
        reentering = true;
        try {
            r.run();
        } finally {
            reentering = false;
        }
    }

    public static Incoming incoming(ChatComponent chat, Component message, GuiMessageTag tag) {
        Incoming in = IncomingPipeline.handle((ChatComponentAccessor) chat, message, tag);
        pending = in.drop() ? null : IncomingPipeline.lastEntry();
        return in;
    }

    public static boolean display(GuiMessage message) {
        if (pending != null) {
            INDEX.bind(message, pending);
            pending = null;
        }
        current = message;
        SearchQuery q = SearchState.query();
        if (!q.active() || q.matches(plain(message))) return true;
        current = null;
        return false;
    }

    public static GuiMessage decorated(GuiMessage message) {
        return LineDecor.decorate(message, INDEX.entry(message));
    }

    public static void line(GuiMessage.Line line) {
        INDEX.line(line, current);
    }

    public static int historySize(int vanilla) {
        ChatModule m = ChatModule.get();
        return m != null && m.isEnabled() ? HistorySize.clamp(m.history.get()) : vanilla;
    }

    public static void tick(ChatComponent chat) {
        ChatRefresh.tick(chat);
        if (++ticks % PRUNE_EVERY != 0) return;
        ChatComponentAccessor acc = (ChatComponentAccessor) chat;
        INDEX.prune(acc.vibey$allMessages(), acc.vibey$trimmedMessages());
    }

    public static String plain(GuiMessage message) {
        ChatEntry e = INDEX.entry(message);
        return e != null ? e.plain() : message.content().getString();
    }

    public static void removeNewest(ChatComponentAccessor chat, GuiMessage newest) {
        var all = chat.vibey$allMessages();
        if (all.isEmpty() || all.get(0) != newest) return;
        all.remove(0);
        var lines = chat.vibey$trimmedMessages();
        while (!lines.isEmpty() && INDEX.owner(lines.get(0)) == newest) lines.remove(0);
        INDEX.forget(newest);
    }
}
