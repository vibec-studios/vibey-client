package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Controls;
import dev.henny.vui.component.text.TextEditState;
import dev.henny.vui.dom.Element;
import dev.henny.vui.render.TextMetrics;

final class TextFieldGeometry {
    static final char MASK = '•';

    private TextFieldGeometry() {
    }

    static boolean masked(Element e) {
        return Controls.bool(e, "password");
    }

    static float widthTo(Element e, TextEditState s, int index, TextMetrics m) {
        if (masked(e)) return m.glyphWidth(MASK, false) * index;
        return m.width(s.chars(), 0, index, false);
    }

    static int indexAt(Element e, TextEditState s, float screenX, TextMetrics m) {
        float local = LocalPoint.x(e, screenX) - e.box().borderLeft - e.box().paddingLeft + s.scroll;
        float pen = 0;
        CharSequence chars = s.chars();
        for (int i = 0; i < chars.length(); i++) {
            float w = masked(e) ? m.glyphWidth(MASK, false) : m.glyphWidth(chars.charAt(i), false);
            if (local < pen + w / 2f) return i;
            pen += w;
        }
        return chars.length();
    }

    static void keepCaretVisible(Element e, TextEditState s, TextMetrics m) {
        float caretX = widthTo(e, s, s.caret(), m);
        float view = e.box().innerWidth() - 1;
        if (caretX - s.scroll > view) s.scroll = caretX - view;
        if (caretX - s.scroll < 0) s.scroll = caretX;
        float total = widthTo(e, s, s.length(), m);
        if (total - s.scroll < view) s.scroll = Math.max(0, total - view);
    }
}
