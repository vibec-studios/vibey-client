package dev.henny.vui.component;

import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.StaticChild;

public final class Internal {
    private Internal() {
    }

    public static Element child(Element parent, String tag) {
        Element c = create(parent, tag);
        parent.sources().add(new StaticChild(c));
        parent.rebuildChildren();
        return c;
    }

    public static Element create(Element parent, String tag) {
        Behaviors behaviors = parent.document() != null ? parent.document().vui().behaviors() : Behaviors.standard();
        Element c = new Element(tag, behaviors.get(tag), parent.pos());
        c.flags().internal = true;
        c.scope(parent.scope());
        return c;
    }

    public static Element label(Element parent, String text) {
        Element label = child(parent, "text");
        label.prop("text", text);
        return label;
    }

    public static Element find(Element parent, String tag) {
        for (int i = 0; i < parent.childCount(); i++) {
            Element c = parent.child(i);
            if (c.flags().internal && c.tag().equals(tag)) return c;
        }
        return null;
    }

    public static void clear(Element parent, String tag) {
        parent.sources().removeIf(s -> {
            if (s instanceof StaticChild sc && sc.element().flags().internal && sc.element().tag().equals(tag)) {
                sc.dispose();
                return true;
            }
            return false;
        });
        parent.rebuildChildren();
    }

    public static boolean hasAuthoredChildren(Element parent) {
        for (int i = 0; i < parent.childCount(); i++) if (!parent.child(i).flags().internal) return true;
        return false;
    }
}
