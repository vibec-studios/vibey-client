package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

final class GridLayout {
    private final LayoutEngine engine;
    private final GridPlacement grid = new GridPlacement();

    GridLayout(LayoutEngine engine) {
        this.engine = engine;
    }

    void measure(Element e, float innerMaxW, float innerMaxH, Size out) {
        ComputedStyle s = e.style();
        float colGap = Lengths.orZero(s, Prop.COLUMN_GAP, innerMaxW);
        float rowGap = Lengths.orZero(s, Prop.ROW_GAP, innerMaxW);
        if (!Float.isFinite(innerMaxW)) innerMaxW = naturalWidth(e, s, innerMaxH, colGap);
        int cols = columns(s, innerMaxW, colGap);
        float cell = cellWidth(innerMaxW, cols, colGap);
        rowHeights(e, cols, cell, colGap, rowGap, false);
        out.set(innerMaxW, grid.totalHeight(rowGap));
    }

    void arrange(Element e) {
        LayoutBox b = e.box();
        ComputedStyle s = e.style();
        float iw = b.innerWidth();
        float colGap = Lengths.orZero(s, Prop.COLUMN_GAP, iw);
        float rowGap = Lengths.orZero(s, Prop.ROW_GAP, iw);
        int cols = columns(s, iw, colGap);
        float cell = cellWidth(iw, cols, colGap);
        rowHeights(e, cols, cell, colGap, rowGap, true);
        for (int k = 0; k < grid.count; k++) {
            Element c = e.child(grid.child[k]);
            LayoutBox cb = c.box();
            float area = grid.spanHeight(k, rowGap);
            boolean stretch = Flow.align(c, e).equals("stretch") && !AspectRatio.has(c);
            float h = Lengths.resolve(c.style(), Prop.HEIGHT, area);
            if (Float.isNaN(h)) h = stretch ? area - cb.marginTop - cb.marginBottom : cb.measuredHeight;
            float x = b.innerX() + grid.col[k] * (cell + colGap) + cb.marginLeft;
            float y = b.innerY() + grid.rowTop(grid.row[k], rowGap) + cb.marginTop;
            float w = grid.spanWidth(k, cell, colGap) - cb.marginLeft - cb.marginRight;
            engine.arrange(c, x, y, w, h);
            engine.extend(b, x + w + cb.marginRight, y + h + cb.marginBottom);
        }
    }

    private void rowHeights(Element e, int cols, float cell, float colGap, float rowGap, boolean fixedHeights) {
        grid.place(e, cols);
        grid.clearHeights();
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < grid.count; k++) {
                boolean single = grid.rowSpan[k] == 1;
                if (single != (pass == 0)) continue;
                Element c = e.child(grid.child[k]);
                engine.measure(c, grid.spanWidth(k, cell, colGap), Float.POSITIVE_INFINITY);
                float h = fixedHeights ? Lengths.resolve(c.style(), Prop.HEIGHT, Float.NaN) : Float.NaN;
                float outer = Flow.outerHeight(c.box(), Float.isNaN(h) ? c.box().measuredHeight : h);
                if (single) grid.rowHeight[grid.row[k]] = Math.max(grid.rowHeight[grid.row[k]], outer);
                else spread(k, outer, rowGap);
            }
        }
    }

    private void spread(int k, float outer, float rowGap) {
        float have = grid.spanHeight(k, rowGap);
        if (outer <= have) return;
        float extra = (outer - have) / grid.rowSpan[k];
        for (int r = grid.row[k]; r < grid.row[k] + grid.rowSpan[k]; r++) grid.rowHeight[r] += extra;
    }

    private float naturalWidth(Element e, ComputedStyle s, float innerMaxH, float colGap) {
        float widest = 0;
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            engine.measure(c, Float.POSITIVE_INFINITY, innerMaxH);
            widest = Math.max(widest, Flow.outerWidth(c.box(), c.box().measuredWidth));
        }
        return widest * Math.max(1, columns(s, widest * 4, colGap)) + colGap * 3;
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
