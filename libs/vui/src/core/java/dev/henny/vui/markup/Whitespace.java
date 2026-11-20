package dev.henny.vui.markup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Whitespace {
    public static final Set<String> RAW_TEXT_TAGS = Set.of("style", "script", "props", "contract");

    private Whitespace() {
    }

    public static List<TemplateNode> condense(List<TemplateNode> nodes) {
        List<TemplateNode> out = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            TemplateNode node = nodes.get(i);
            if (node instanceof ElementNode e) {
                out.add(RAW_TEXT_TAGS.contains(e.tag()) ? e : new ElementNode(e.tag(), e.attributes(), condense(e.children()), e.pos()));
                continue;
            }
            TextNode t = (TextNode) node;
            if (isBlank(t)) continue;
            out.add(collapse(t, i == 0, i == nodes.size() - 1));
        }
        return out;
    }

    private static boolean isBlank(TextNode t) {
        for (TextPart p : t.parts()) if (p.expression() || !p.text().isBlank()) return false;
        return true;
    }

    private static TextNode collapse(TextNode t, boolean first, boolean last) {
        List<TextPart> parts = new ArrayList<>();
        int n = t.parts().size();
        for (int i = 0; i < n; i++) {
            TextPart p = t.parts().get(i);
            if (p.expression()) {
                parts.add(p);
                continue;
            }
            String s = p.text().replaceAll("\\s+", " ");
            if (first && i == 0) s = s.stripLeading();
            if (last && i == n - 1) s = s.stripTrailing();
            if (!s.isEmpty()) parts.add(new TextPart(false, s, p.pos()));
        }
        return new TextNode(parts, t.pos());
    }
}
