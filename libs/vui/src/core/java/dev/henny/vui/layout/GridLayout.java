package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

final class GridLayout {
    private final LayoutEngine engine;

    GridLayout(LayoutEngine engine) {
        this.engine = engine;
    }

    void measure(Element e, float innerMaxW, float innerMaxH, Size out) {
        ComputedStyle s = e.style();
        float colGap = Lengths.orZero(s, Prop.COLUMN_GAP, innerMaxW);
        float rowGap = Lengths.orZero(s, Prop.ROW_GAP, innerMaxW);
        if (!Float.isFinite(innerMaxW)) {
            float widest = 0;
            for (int i = 0; i < e.childCount(); i++) {
                Element c = e.child(i);
                if (!Flow.inFlow(c)) continue;
                engine.measure(c, Float.POSITIVE_INFINITY, innerMaxH);
                widest = Math.max(widest, Flow.outerWidth(c.box(), c.box().measuredWidth));
            }
            innerMaxW = widest * Math.max(1, columns(s, widest * 4, colGap)) + colGap * 3;
        }
        int cols = columns(s, innerMaxW, colGap);
        float cell = cellWidth(innerMaxW, cols, colGap);
        float height = 0;
        float rowHeight = 0;
        int col = 0;
        int rows = 0;
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            engine.measure(c, cell, Float.POSITIVE_INFINITY);
            rowHeight = Math.max(rowHeight, Flow.outerHeight(c.box(), c.box().measuredHeight));
            if (++col == cols) {
                height += rowHeight + (rows > 0 ? rowGap : 0);
                rows++;
                rowHeight = 0;
                col = 0;
            }
        }
        if (col > 0) height += rowHeight + (rows > 0 ? rowGap : 0);
        out.set(innerMaxW, height);
    }

    void arrange(Element e) {
        LayoutBox b = e.box();
        ComputedStyle s = e.style();
        float iw = b.innerWidth();
        float colGap = Lengths.orZero(s, Prop.COLUMN_GAP, iw);
        float rowGap = Lengths.orZero(s, Prop.ROW_GAP, iw);
        int cols = columns(s, iw, colGap);
        float cell = cellWidth(iw, cols, colGap);
        float y = b.innerY();
        int rowStart = 0;
        int col = 0;
        float rowHeight = 0;
        for (int i = 0; i <= e.childCount(); i++) {
            boolean end = i == e.childCount();
            if (!end) {
                Element c = e.child(i);
                if (!Flow.inFlow(c)) continue;
                engine.measure(c, cell, Float.POSITIVE_INFINITY);
                float h = Lengths.resolve(c.style(), Prop.HEIGHT, Float.NaN);
                rowHeight = Math.max(rowHeight, Flow.outerHeight(c.box(), Float.isNaN(h) ? c.box().measuredHeight : h));
                col++;
            }
            if (col == cols || end && col > 0) {
                placeRow(e, rowStart, end ? i : i + 1, y, cell, colGap, rowHeight);
                y += rowHeight + rowGap;
                rowStart = i + 1;
                col = 0;
                rowHeight = 0;
            }
        }
    }

    private void placeRow(Element e, int from, int to, float y, float cell, float colGap, float rowHeight) {
        LayoutBox b = e.box();
        int col = 0;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            LayoutBox cb = c.box();
            boolean stretch = Flow.align(c, e).equals("stretch");
            float h = Lengths.resolve(c.style(), Prop.HEIGHT, rowHeight);
            if (Float.isNaN(h)) h = stretch ? rowHeight - cb.marginTop - cb.marginBottom : cb.measuredHeight;
            float x = b.innerX() + col * (cell + colGap) + cb.marginLeft;
            float w = cell - cb.marginLeft - cb.marginRight;
            engine.arrange(c, x, y + cb.marginTop, w, h);
            engine.extend(b, x + w + cb.marginRight, y + cb.marginTop + h + cb.marginBottom);
            col++;
        }
    }

    private static int columns(ComputedStyle s, float width, float gap) {
        int fixed = (int) s.number(Prop.GRID_COLUMNS);
        if (fixed > 0) return fixed;
        float min = Lengths.orZero(s, Prop.GRID_MIN_COLUMN, width);
        if (min <= 0 || !Float.isFinite(width)) return 1;
        return Math.max(1, (int) Math.floor((width + gap) / (min + gap)));
    }

    private static float cellWidth(float width, int cols, float gap) {
        return Math.max(0, (width - gap * (cols - 1)) / cols);
    }
}
