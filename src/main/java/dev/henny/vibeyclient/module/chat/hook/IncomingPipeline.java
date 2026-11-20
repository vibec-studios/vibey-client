package dev.henny.vibeyclient.module.chat.hook;

import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.ChatEntry;
import dev.henny.vibeyclient.module.chat.core.DirectMessage;
import dev.henny.vibeyclient.module.chat.core.SpamCollapse;
import dev.henny.vibeyclient.module.chat.line.MentionTag;
import dev.henny.vibeyclient.module.chat.line.Mentions;
import dev.henny.vibeyclient.module.chat.mixin.ChatComponentAccessor;
import dev.henny.vibeyclient.module.chat.notify.ChatNotifier;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class IncomingPipeline {
    private static ChatEntry last;

    private IncomingPipeline() {}

    public static ChatEntry lastEntry() {
        return last;
    }

    public static Incoming handle(ChatComponentAccessor chat, Component message, GuiMessageTag tag) {
        String plain = message.getString();
        long now = System.currentTimeMillis();
        ChatModule m = ChatModule.get();
        if (m == null || !m.isEnabled()) {
            last = ChatEntry.of(plain, now, false, null);
            return Incoming.keep(tag);
        }
        if (m.filters.on() && m.filterStore().filters().hides(plain)) {
            m.filterStore().countHidden();
            return Incoming.dropped();
        }
        if (m.collapse.on()) {
            List<GuiMessage> all = chat.vibey$allMessages();
            GuiMessage newest = all.isEmpty() ? null : all.get(0);
            ChatEntry prev = ChatHooks.INDEX.entry(newest);
            if (SpamCollapse.repeats(prev, plain, now)) {
                ChatHooks.removeNewest(chat, newest);
                last = prev.repeated(now);
                return Incoming.keep(tag);
            }
        }
        DirectMessage dm = DmDetector.detect(message, plain, Mentions.ownName());
        boolean mention = dm == null && m.highlight.on() && Mentions.matcher(m).mentions(plain);
        last = ChatEntry.of(plain, now, mention, dm == null ? null : dm.sender());
        ChatNotifier.notify(m, dm, mention, plain);
        return Incoming.keep(mention && MentionTag.replaces(tag) ? MentionTag.TAG : tag);
    }
}
