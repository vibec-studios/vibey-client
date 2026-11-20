package dev.henny.vibeyclient.client.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class StyledText {
    private record Piece(String text, Style style) {}

    private StyledText() {}

    public static Component apply(Component source, List<TextEdit> edits) {
        if (edits.isEmpty()) return source;
        List<TextEdit> sorted = new ArrayList<>(edits);
        sorted.sort(Comparator.comparingInt(TextEdit::start));
        List<Piece> out = new ArrayList<>();
        int[] offset = {0};
        int[] next = {0};
        source.visit((style, text) -> {
            emit(text, style, offset[0], sorted, next, out);
            offset[0] += text.length();
            return Optional.empty();
        }, Style.EMPTY);
        return build(out);
    }

    public static Component restyleAll(Component source, UnaryOperator<Style> restyle) {
        List<Piece> out = new ArrayList<>();
        source.visit((style, text) -> {
            out.add(new Piece(text, restyle.apply(style)));
            return Optional.empty();
        }, Style.EMPTY);
        return build(out);
    }

    private static void emit(String text, Style style, int base, List<TextEdit> edits, int[] next, List<Piece> out) {
        int at = 0;
        int len = text.length();
        while (at < len) {
            int pos = base + at;
            while (next[0] < edits.size() && edits.get(next[0]).end() <= pos) next[0]++;
            TextEdit e = next[0] < edits.size() ? edits.get(next[0]) : null;
            if (e == null || e.start() >= base + len) {
                out.add(new Piece(text.substring(at), style));
                return;
            }
            if (e.start() > pos) {
                int cut = e.start() - base;
                out.add(new Piece(text.substring(at, cut), style));
                at = cut;
                continue;
            }
            int stop = Math.min(len, e.end() - base);
            if (e.replacement() != null) {
                if (e.start() >= base + at && e.start() == pos) out.add(new Piece(e.replacement(), style));
            } else {
                out.add(new Piece(text.substring(at, stop), e.restyle().apply(style)));
            }
            at = stop;
        }
    }

    private static Component build(List<Piece> pieces) {
        MutableComponent root = Component.empty();
        for (Piece p : pieces) if (!p.text().isEmpty()) root.append(Component.literal(p.text()).setStyle(p.style()));
        return root;
    }
}
