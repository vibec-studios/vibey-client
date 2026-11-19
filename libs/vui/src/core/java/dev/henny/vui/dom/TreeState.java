package dev.henny.vui.dom;

import dev.henny.vui.event.FocusManager;
import dev.henny.vui.layout.Flow;

import java.util.HashMap;
import java.util.Map;

public final class TreeState {
    private final Map<String, float[]> scroll = new HashMap<>();
    private String focusedPath;

    public static TreeState capture(Document doc) {
        TreeState s = new TreeState();
        s.collect(doc.root(), "");
        Element focused = doc.events().focus().focused();
        if (focused != null) s.focusedPath = path(focused);
        return s;
    }

    public void restore(Document doc) {
        apply(doc.root(), "", doc);
        doc.invalidateLayout();
        doc.layoutIfNeeded();
    }

    public int scrollCount() {
        return scroll.size();
    }

    private void collect(Element e, String parentPath) {
        String p = parentPath + "/" + segment(e);
        if (Flow.scrolls(e) && (e.scrollX() != 0 || e.scrollY() != 0)) scroll.put(p, new float[]{e.scrollX(), e.scrollY()});
        for (int i = 0; i < e.childCount(); i++) collect(e.child(i), p);
    }

    private void apply(Element e, String parentPath, Document doc) {
        String p = parentPath + "/" + segment(e);
        float[] s = scroll.get(p);
        if (s != null) e.restoreScroll(s[0], s[1]);
        if (p.equals(focusedPath) && FocusManager.focusable(e)) doc.events().focus().focus(e, false);
        for (int i = 0; i < e.childCount(); i++) apply(e.child(i), p, doc);
    }

    static String path(Element e) {
        StringBuilder sb = new StringBuilder();
        for (Element p = e; p != null; p = p.parent()) sb.insert(0, "/" + segment(p));
        return sb.toString();
    }

    private static String segment(Element e) {
        if (e.key() != null) return e.tag() + "[k=" + e.key() + "]";
        if (e.id() != null) return e.tag() + "#" + e.id();
        Element parent = e.parent();
        int index = 0;
        if (parent != null) {
            for (int i = 0; i < parent.childCount(); i++) {
                Element c = parent.child(i);
                if (c == e) break;
                if (c.tag().equals(e.tag())) index++;
            }
        }
        return e.tag() + "[" + index + "]";
    }
}
