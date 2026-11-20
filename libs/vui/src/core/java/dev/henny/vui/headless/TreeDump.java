package dev.henny.vui.headless;

import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.Flow;
import dev.henny.vui.style.StateFlags;

public final class TreeDump {
    private TreeDump() {
    }

    public static String dump(Element root) {
        StringBuilder sb = new StringBuilder();
        line(root, 0, sb);
        return sb.toString();
    }

    private static void line(Element e, int depth, StringBuilder sb) {
        if (e.tag().equals(Element.TEXT_TAG)) return;
        sb.append("  ".repeat(depth)).append(e.tag());
        if (e.id() != null) sb.append('#').append(e.id());
        for (String c : e.classes().all()) sb.append('.').append(c);
        String states = StateFlags.names(e.states() & ~(StateFlags.FIRST | StateFlags.LAST));
        sb.append(states).append(' ').append(e.box());
        if (!Flow.displayed(e)) sb.append(" hidden");
        if (e.flags().overlay) sb.append(e.flags().overlayOpen ? " overlay:open" : " overlay:closed");
        if (e.text() != null) sb.append(" \"").append(Values.display(e.text().chars())).append('"');
        Object value = e.prop("value");
        if (value != null && e.text() == null) sb.append(" value=").append(Values.display(value));
        sb.append("  @").append(e.pos()).append('\n');
        if (e.text() != null) return;
        for (int i = 0; i < e.childCount(); i++) line(e.child(i), depth + 1, sb);
    }
}
