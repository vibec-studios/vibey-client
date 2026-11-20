package dev.henny.vibeyclient.module.chat.line;

import dev.henny.vibeyclient.api.privacy.NameRedactor;
import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.client.text.StyledText;
import dev.henny.vibeyclient.client.text.TextEdit;
import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.ChatEntry;
import dev.henny.vibeyclient.module.chat.core.Ranges;
import dev.henny.vibeyclient.module.chat.core.SearchQuery;
import dev.henny.vibeyclient.module.chat.core.SpamCollapse;
import dev.henny.vibeyclient.module.chat.core.Timestamps;
import dev.henny.vibeyclient.module.chat.screen.SearchState;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public final class LineDecor {
    private LineDecor() {}

    public static GuiMessage decorate(GuiMessage message, ChatEntry entry) {
        Component c = message.content();
        NameRedactor redactor = Privacy.redactor();
        if (redactor.active()) c = Redact.names(c, redactor);
        ChatModule m = ChatModule.get();
        if (m != null && m.isEnabled()) c = features(m, c, entry);
        return c == message.content() ? message : new GuiMessage(message.addedTime(), c, message.signature(), message.tag());
    }

    private static Component features(ChatModule m, Component c, ChatEntry entry) {
        Component out = c;
        if (entry != null && entry.mention() && m.highlight.on()) {
            out = restyle(out, Mentions.highlighter(m).ranges(out.getString()), LineStyles::mention);
        }
        SearchQuery q = SearchState.query();
        if (q.active()) out = restyle(out, q.ranges(out.getString()), LineStyles::search);
        if (entry == null) return out;
        boolean time = m.timestamps.on();
        boolean count = entry.count() > 1;
        if (!time && !count) return out;
        MutableComponent line = Component.empty();
        if (time) line.append(Component.literal(Timestamps.bracketed(entry.at(), m.timeStyle.get(), ZoneId.systemDefault()))
                .setStyle(LineStyles.dim(LineStyles.TIME)));
        line.append(out);
        if (count) line.append(Component.literal(SpamCollapse.suffix(entry.count())).setStyle(LineStyles.dim(LineStyles.COUNT)));
        return line;
    }

    private static Component restyle(Component c, List<Ranges.Range> ranges, UnaryOperator<Style> style) {
        if (ranges.isEmpty()) return c;
        List<TextEdit> edits = new ArrayList<>(ranges.size());
        for (Ranges.Range r : ranges) edits.add(TextEdit.restyle(r.start(), r.end(), style));
        return StyledText.apply(c, edits);
    }
}
