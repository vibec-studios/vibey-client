package dev.henny.vui.mc.dev;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.ScreenRects;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.TextMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class InspectorPainter {
    private static final int OUTLINE = 0xFF8B6CFF;
    private static final int PADDING = 0x303FE0C5;
    private static final int PANEL = 0xFF0B0B0E;
    private static final int BORDER = 0xFF3FE0C5;
    private static final int TEXT = 0xFFF4F4F5;
    private static final int DIM = 0xFFA1A1AA;

    private InspectorPainter() {
    }

    static void paint(Canvas c, TextMetrics m, Document doc, Element e, float mx, float my, float sw, float sh) {
        List<String> lines = new ArrayList<>();
        if (e == null) {
            lines.add("inspect: point at a vui element");
            lines.add("Ctrl+Shift+I closes");
        } else {
            Rect r = ScreenRects.of(e);
            outline(c, r, e.box());
            lines.add(selector(e));
            lines.add(e.pos().shortText());
            lines.add(box(r, e.box()));
            lines.add(doc.documentId());
        }
        label(c, m, lines, mx, my, sw, sh);
    }

    private static void outline(Canvas c, Rect r, LayoutBox b) {
        c.fill(r.x() + b.borderLeft, r.y() + b.borderTop, b.paddingLeft, r.height() - b.borderTop - b.borderBottom, PADDING);
        c.fill(r.right() - b.borderRight - b.paddingRight, r.y() + b.borderTop, b.paddingRight, r.height() - b.borderTop - b.borderBottom, PADDING);
        c.fill(r.x(), r.y(), r.width(), 1, OUTLINE);
        c.fill(r.x(), r.bottom() - 1, r.width(), 1, OUTLINE);
        c.fill(r.x(), r.y(), 1, r.height(), OUTLINE);
        c.fill(r.right() - 1, r.y(), 1, r.height(), OUTLINE);
    }

    private static String selector(Element e) {
        StringBuilder sb = new StringBuilder("<").append(e.tag()).append('>');
        if (e.id() != null) sb.append('#').append(e.id());
        for (String cls : e.classes().all()) sb.append('.').append(cls);
        return sb.toString();
    }

    private static String box(Rect r, LayoutBox b) {
        return String.format(Locale.ROOT, "%.0f,%.0f  %.0f x %.0f  pad %.0f %.0f %.0f %.0f", r.x(), r.y(), r.width(), r.height(),
                b.paddingTop, b.paddingRight, b.paddingBottom, b.paddingLeft);
    }

    private static void label(Canvas c, TextMetrics m, List<String> lines, float mx, float my, float sw, float sh) {
        float lh = m.lineHeight() + 1;
        float w = 0;
        for (String s : lines) w = Math.max(w, m.width(s));
        float pw = w + 8;
        float ph = lines.size() * lh + 6;
        float x = Math.min(mx + 12, sw - pw - 2);
        float y = Math.min(my + 12, sh - ph - 2);
        x = Math.max(2, x);
        y = Math.max(2, y);
        c.fill(x, y, pw, ph, PANEL);
        c.fill(x, y, pw, 1, BORDER);
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            c.text(s, 0, s.length(), x + 4, y + 4 + i * lh, i == 0 ? TEXT : DIM, false, false);
        }
    }
}
