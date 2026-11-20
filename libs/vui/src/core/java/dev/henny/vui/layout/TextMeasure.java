package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.TextContent;
import dev.henny.vui.render.TextMetrics;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.Unit;

final class TextMeasure {
    private final LayoutEngine engine;

    TextMeasure(LayoutEngine engine) {
        this.engine = engine;
    }

    void measure(Element e, float innerMaxW, Size out) {
        TextLines lines = wrap(e, innerMaxW);
        float scale = scale(e);
        int count = Math.max(1, lines.count());
        out.set(lines.maxWidth() * scale, count * lineHeight(e) * scale);
    }

    void arrange(Element e) {
        wrap(e, e.box().innerWidth());
    }

    private TextLines wrap(Element e, float innerMaxW) {
        TextContent text = e.text();
        TextMetrics m = engine.metrics();
        text.metrics(m);
        float scale = scale(e);
        float avail = Float.isFinite(innerMaxW) ? innerMaxW / scale : Float.POSITIVE_INFINITY;
        if (!text.linesValidFor(avail)) {
            ComputedStyle s = e.style();
            boolean wrap = s.is(Prop.TEXT_WRAP, "wrap") && Float.isFinite(avail);
            boolean ellipsis = s.is(Prop.TEXT_OVERFLOW, "ellipsis") && Float.isFinite(avail);
            int maxLines = (int) s.number(Prop.MAX_LINES);
            float ellipsisWidth = m.width(TextWrapper.ELLIPSIS, 0, 1, s.is(Prop.FONT_WEIGHT, "bold"));
            TextWrapper.wrap(text.chars(), text, avail, wrap, maxLines, ellipsis, ellipsisWidth, text.lines());
            text.linesComputed(avail, m);
        }
        return text.lines();
    }

    static float scale(Element e) {
        float s = e.style().number(Prop.FONT_SCALE);
        return s <= 0 ? 1f : s;
    }

    float lineHeight(Element e) {
        ComputedStyle s = e.style();
        if (s.unit(Prop.LINE_HEIGHT) == Unit.PX) return s.number(Prop.LINE_HEIGHT) / scale(e);
        return engine.metrics().lineHeight();
    }
}
