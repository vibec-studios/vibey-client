package dev.henny.vui.dom;

import dev.henny.vui.style.Selector;
import dev.henny.vui.style.SelectorParser;

import java.util.ArrayList;
import java.util.List;

public final class Queries {
    private Queries() {
    }

    public static Element byId(Element root, String id) {
        if (id.equals(root.id())) return root;
        for (int i = 0; i < root.childCount(); i++) {
            Element found = byId(root.child(i), id);
            if (found != null) return found;
        }
        return null;
    }

    public static List<Element> select(Element root, String selector) {
        Selector s = SelectorParser.parse(selector);
        List<Element> out = new ArrayList<>();
        collect(root, s, out);
        return out;
    }

    public static Element first(Element root, String selector) {
        List<Element> all = select(root, selector);
        return all.isEmpty() ? null : all.get(0);
    }

    private static void collect(Element e, Selector s, List<Element> out) {
        if (s.matches(e)) out.add(e);
        for (int i = 0; i < e.childCount(); i++) collect(e.child(i), s, out);
    }

    public static Element byPos(Element root, String file, int line) {
        Element best = null;
        if (root.pos().file().equals(file) && root.pos().line() == line) best = root;
        for (int i = 0; i < root.childCount() && best == null; i++) best = byPos(root.child(i), file, line);
        return best;
    }
}
