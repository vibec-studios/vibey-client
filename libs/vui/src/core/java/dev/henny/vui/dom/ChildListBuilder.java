package dev.henny.vui.dom;

import dev.henny.vui.style.StateFlags;

import java.util.ArrayList;
import java.util.List;

final class ChildListBuilder {
    private static final List<Element> SCRATCH = new ArrayList<>();

    private ChildListBuilder() {
    }

    static void rebuild(Element parent) {
        List<Element> next = SCRATCH;
        next.clear();
        for (int i = 0; i < parent.sources().size(); i++) parent.sources().get(i).collect(next);
        List<Element> children = parent.children();
        boolean changed = !children.equals(next);
        children.clear();
        children.addAll(next);
        next.clear();
        int last = children.size() - 1;
        boolean siblings = changed && parent.document() != null && parent.document().styles().usesSiblings();
        for (int i = 0; i <= last; i++) {
            Element c = children.get(i);
            c.parent(parent);
            c.siblingIndex = i;
            if (siblings) c.markSubtreeStyleDirty();
            if (c.document() != parent.document()) c.attach(parent.document());
            if (!c.flags().styledOnce) c.markStyleDirty();
            c.state(StateFlags.FIRST, i == 0);
            c.state(StateFlags.LAST, i == last);
            if (parent.document() != null && !c.flags().mounted) Lifecycle.mount(c);
        }
        if (changed) parent.markLayoutDirty();
    }
}
