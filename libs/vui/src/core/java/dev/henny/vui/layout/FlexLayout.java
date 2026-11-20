package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

final class FlexLayout {
    private final LayoutEngine engine;
    private final FlexLines lines = new FlexLines();

    FlexLayout(LayoutEngine engine) {
        this.engine = engine;
    }

    void measure(Element e, float innerMaxW, float innerMaxH, Size out) {
        boolean row = Flow.isRow(e);
        ComputedStyle s = e.style();
        float gapMain = Lengths.orZero(s, row ? Prop.COLUMN_GAP : Prop.ROW_GAP, innerMaxW);
        float gapCross = Lengths.orZero(s, row ? Prop.ROW_GAP : Prop.COLUMN_GAP, innerMaxW);
        float maxMain = row ? innerMaxW : innerMaxH;
        boolean wrap = s.is(Prop.WRAP, "wrap") && Float.isFinite(maxMain);
        float main = 0;
        float cross = 0;
        float lineMain = 0;
        float lineCross = 0;
        int inLine = 0;
        int lineCount = 0;
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            engine.measure(c, innerMaxW, Flow.scrolls(e) && !row ? Float.POSITIVE_INFINITY : innerMaxH);
            LayoutBox cb = c.box();
            float cm = row ? Flow.outerWidth(cb, cb.measuredWidth) : Flow.outerHeight(cb, cb.measuredHeight);
            float cc = row ? Flow.outerHeight(cb, cb.measuredHeight) : Flow.outerWidth(cb, cb.measuredWidth);
            if (wrap && inLine > 0 && lineMain + gapMain + cm > maxMain) {
                cross += lineCross + (lineCount > 0 ? gapCross : 0);
                lineCount++;
                lineMain = 0;
                lineCross = 0;
                inLine = 0;
            }
            lineMain += (inLine > 0 ? gapMain : 0) + cm;
            lineCross = Math.max(lineCross, cc);
            inLine++;
            main = Math.max(main, lineMain);
        }
        if (inLine > 0) cross += lineCross + (lineCount > 0 ? gapCross : 0);
        if (row) out.set(main, cross);
        else out.set(cross, main);
    }

    void arrange(Element e) {
        LayoutBox b = e.box();
        ComputedStyle s = e.style();
        boolean row = Flow.isRow(e);
        float innerW = b.innerWidth();
        float innerH = b.innerHeight();
        float mainSize = row ? innerW : innerH;
        float crossSize = row ? innerH : innerW;
        boolean scrollMain = Flow.scrolls(e);
        float gapMain = Lengths.orZero(s, row ? Prop.COLUMN_GAP : Prop.ROW_GAP, innerW);
        float gapCross = Lengths.orZero(s, row ? Prop.ROW_GAP : Prop.COLUMN_GAP, innerW);
        boolean wrap = s.is(Prop.WRAP, "wrap") && !scrollMain;
        resolveBases(e, row, innerW, innerH, scrollMain);
        lines.split(e, row, mainSize, gapMain, wrap);
        float crossOffset = 0;
        for (int l = 0; l < lines.count(); l++) {
            int from = lines.start(l);
            int to = lines.end(l);
            if (!scrollMain) FlexSizing.distribute(e, from, to, row, mainSize, gapMain);
            float lineCross = crossSizes(e, from, to, row, crossSize, lines.count() == 1 && !wrap);
            position(e, from, to, row, mainSize, gapMain, crossOffset, lineCross);
            crossOffset += lineCross + gapCross;
        }
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            LayoutBox cb = c.box();
            engine.arrange(c, cb.x, cb.y, cb.width, cb.height);
            engine.extend(b, cb.x + cb.width + cb.marginRight, cb.y + cb.height + cb.marginBottom);
        }
    }

    private void resolveBases(Element e, boolean row, float innerW, float innerH, boolean scrollMain) {
        float mainSize = row ? innerW : innerH;
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            ComputedStyle cs = c.style();
            LayoutBox cb = c.box();
            Lengths.resolveBox(cb, cs, innerW);
            float basis = Lengths.resolve(cs, Prop.BASIS, mainSize);
            if (Float.isNaN(basis)) basis = Lengths.resolve(cs, row ? Prop.WIDTH : Prop.HEIGHT, mainSize);
            if (Float.isNaN(basis)) {
                engine.measure(c, innerW, scrollMain && !row ? Float.POSITIVE_INFINITY : innerH);
                basis = row ? cb.measuredWidth : cb.measuredHeight;
            }
            float min = Lengths.orZero(cs, row ? Prop.MIN_WIDTH : Prop.MIN_HEIGHT, mainSize);
            float max = Lengths.max(cs, row ? Prop.MAX_WIDTH : Prop.MAX_HEIGHT, mainSize);
            cb.flexBasis = Lengths.clamp(basis, min, max);
            cb.flexMain = cb.flexBasis;
        }
    }

    private float crossSizes(Element e, int from, int to, boolean row, float crossSize, boolean singleLine) {
        float lineCross = 0;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            LayoutBox cb = c.box();
            ComputedStyle cs = c.style();
            float cross = Lengths.resolve(cs, row ? Prop.HEIGHT : Prop.WIDTH, crossSize);
            boolean stretch = Float.isNaN(cross) && Flow.align(c, e).equals("stretch") && !AutoMargins.anyCross(c, row)
                    && !(row && AspectRatio.has(c));
            if (Float.isNaN(cross)) {
                engine.measure(c, row ? cb.flexMain + cb.marginLeft + cb.marginRight : crossSize,
                        row ? crossSize : cb.flexMain + cb.marginTop + cb.marginBottom);
                cross = row ? cb.measuredHeight : cb.measuredWidth;
            }
            cb.flexCross = cross;
            cb.flexFrozen = stretch;
            float outer = cross + (row ? cb.marginTop + cb.marginBottom : cb.marginLeft + cb.marginRight);
            lineCross = Math.max(lineCross, outer);
        }
        if (singleLine && Float.isFinite(crossSize)) lineCross = crossSize;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c) || !c.box().flexFrozen) continue;
            LayoutBox cb = c.box();
            float margins = row ? cb.marginTop + cb.marginBottom : cb.marginLeft + cb.marginRight;
            ComputedStyle cs = c.style();
            float min = Lengths.orZero(cs, row ? Prop.MIN_HEIGHT : Prop.MIN_WIDTH, crossSize);
            float max = Lengths.max(cs, row ? Prop.MAX_HEIGHT : Prop.MAX_WIDTH, crossSize);
            cb.flexCross = Lengths.clamp(lineCross - margins, min, max);
        }
        return lineCross;
    }

    private void position(Element e, int from, int to, boolean row, float mainSize, float gap, float crossOffset, float lineCross) {
        LayoutBox b = e.box();
        int count = 0;
        float used = 0;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            LayoutBox cb = c.box();
            used += cb.flexMain + (row ? cb.marginLeft + cb.marginRight : cb.marginTop + cb.marginBottom);
            count++;
        }
        used += gap * Math.max(0, count - 1);
        float free = Float.isFinite(mainSize) ? mainSize - used : 0;
        int autos = free > 0 ? AutoMargins.countMain(e, from, to, row) : 0;
        float autoShare = autos > 0 ? free / autos : 0;
        Justify j = Justify.of(autos > 0 ? "start" : e.style().keyword(Prop.JUSTIFY), autos > 0 ? 0 : free, count, gap);
        float pos = j.start();
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            LayoutBox cb = c.box();
            float mStart = row ? cb.marginLeft : cb.marginTop;
            float mEnd = row ? cb.marginRight : cb.marginBottom;
            float cStart = row ? cb.marginTop : cb.marginLeft;
            float cEnd = row ? cb.marginBottom : cb.marginRight;
            float crossPos = crossOffset + alignOffset(AutoMargins.crossAlign(c, e, row), lineCross, cb.flexCross + cStart + cEnd) + cStart;
            if (autos > 0 && AutoMargins.mainStart(c, row)) mStart += autoShare;
            if (autos > 0 && AutoMargins.mainEnd(c, row)) mEnd += autoShare;
            float mainPos = pos + mStart;
            if (row) cb.set(b.innerX() + mainPos, b.innerY() + crossPos, cb.flexMain, cb.flexCross);
            else cb.set(b.innerX() + crossPos, b.innerY() + mainPos, cb.flexCross, cb.flexMain);
            pos = mainPos + cb.flexMain + mEnd + j.gap();
        }
    }

    private static float alignOffset(String align, float lineCross, float outer) {
        return switch (align) {
            case "center" -> (lineCross - outer) / 2f;
            case "end" -> lineCross - outer;
            default -> 0f;
        };
    }
}
