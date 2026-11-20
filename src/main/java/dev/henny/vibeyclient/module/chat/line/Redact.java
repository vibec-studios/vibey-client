package dev.henny.vibeyclient.module.chat.line;

import dev.henny.vibeyclient.api.privacy.NameRedactor;
import dev.henny.vibeyclient.client.text.StyledText;
import dev.henny.vibeyclient.client.text.TextEdit;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public final class Redact {
    private Redact() {}

    public static Component names(Component source, NameRedactor redactor) {
        List<NameRedactor.Hit> hits = redactor.hits(source.getString());
        List<TextEdit> edits = new ArrayList<>(hits.size());
        for (NameRedactor.Hit h : hits) edits.add(TextEdit.replace(h.start(), h.end(), redactor.alias(h.name())));
        return StyledText.restyleAll(StyledText.apply(source, edits), Redact::scrub);
    }

    public static String plain(String text, NameRedactor redactor) {
        return redactor.active() ? redactor.redact(text) : text;
    }

    private static Style scrub(Style s) {
        Style out = s;
        if (out.getHoverEvent() instanceof HoverEvent.ShowEntity) out = out.withHoverEvent(null);
        if (out.getInsertion() != null) out = out.withInsertion(null);
        return out;
    }
}
