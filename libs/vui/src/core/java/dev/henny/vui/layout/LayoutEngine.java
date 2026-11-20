package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.render.TextMetrics;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

public final class LayoutEngine {
    private final TextMetrics metrics;
    private final FlexLayout flex = new FlexLayout(this);
    private final StackLayout stack = new StackLayout(this);
    private final GridLayout grid = new GridLayout(this);
    private final PositionedLayout positioned = new PositionedLayout(this);
    private final TextMeasure text = new TextMeasure(this);
    private final Size scratch = new Size();
    private float viewportWidth;
    private float viewportHeight;
    private int stamp;
    private int passes;

    public LayoutEngine(TextMetrics metrics) {
        this.metrics = metrics;
    }

    public TextMetrics metrics() {
        return metrics;
    }

    public float viewportWidth() {
        return viewportWidth;
    }

    public float viewportHeight() {
        return viewportHeight;
    }

    public int passes() {
        return passes;
    }

    public void layout(Element root, float width, float height) {
        viewportWidth = width;
        viewportHeight = height;
        stamp++;
        passes++;
        Lengths.resolveBox(root.box(), root.style(), width);
        arrange(root, 0, 0, width, height);
    }

    public void beginPass(float width, float height) {
        viewportWidth = width;
        viewportHeight = height;
        stamp++;
    }

    public void measure(Element e, float maxWidth, float maxHeight) {
        LayoutBox b = e.box();
        if (b.measureStamp == stamp && b.measureMaxWidth == maxWidth && b.measureMaxHeight == maxHeight) return;
        b.measureStamp = stamp;
        b.measureMaxWidth = maxWidth;
        b.measureMaxHeight = maxHeight;
        if (!Flow.displayed(e)) {
            b.measuredWidth = 0;
            b.measuredHeight = 0;
            return;
        }
        ComputedStyle s = e.style();
        Lengths.resolveBox(b, s, maxWidth);
        float w = Lengths.resolve(s, Prop.WIDTH, maxWidth);
        float h = Lengths.resolve(s, Prop.HEIGHT, maxHeight);
        float maxW = Lengths.max(s, Prop.MAX_WIDTH, maxWidth);
        float maxH = Lengths.max(s, Prop.MAX_HEIGHT, maxHeight);
        float minW = Lengths.orZero(s, Prop.MIN_WIDTH, maxWidth);
        float minH = Lengths.orZero(s, Prop.MIN_HEIGHT, maxHeight);
        float ratio = AspectRatio.of(e);
        if (ratio > 0) {
            float aw = AspectRatio.width(ratio, w, h, Math.min(maxWidth - b.marginLeft - b.marginRight, maxW));
            h = AspectRatio.height(ratio, aw, h);
            w = aw;
        }
        float outerMaxW = Float.isNaN(w) ? Math.min(maxWidth - b.marginLeft - b.marginRight, maxW) : w;
        float outerMaxH = Float.isNaN(h) ? Math.min(maxHeight - b.marginTop - b.marginBottom, maxH) : h;
        Size content = scratch;
        content.set(0, 0);
        contentSize(e, Math.max(0, outerMaxW - b.horizontalExtras()), Math.max(0, outerMaxH - b.verticalExtras()), content);
        float mw = Float.isNaN(w) ? content.width + b.horizontalExtras() : w;
        float mh = Float.isNaN(h) ? content.height + b.verticalExtras() : h;
        b.measuredWidth = Lengths.clamp(mw, minW, maxW);
        b.measuredHeight = Lengths.clamp(mh, minH, maxH);
    }

    private void contentSize(Element e, float innerMaxW, float innerMaxH, Size out) {
        float cw;
        float ch;
        if (e.behavior().measure(e, this, innerMaxW, innerMaxH, out)) return;
        if (e.text() != null) {
            text.measure(e, innerMaxW, out);
            return;
        }
        String display = e.style().keyword(Prop.DISPLAY);
        switch (display) {
            case "stack" -> stack.measure(e, innerMaxW, innerMaxH, out);
            case "grid" -> grid.measure(e, innerMaxW, innerMaxH, out);
            default -> flex.measure(e, innerMaxW, innerMaxH, out);
        }
        cw = out.width;
        ch = out.height;
        out.set(cw, ch);
    }

    public void arrange(Element e, float x, float y, float w, float h) {
        LayoutBox b = e.box();
        b.set(x, y, w, h);
        if (!Flow.displayed(e)) return;
        Lengths.resolveBox(b, e.style(), w);
        b.contentWidth = 0;
        b.contentHeight = 0;
        if (e.behavior().arrangeChildren(e, this)) {
            positioned.arrangeAll(e);
            return;
        }
        if (e.text() != null) {
            text.arrange(e);
        } else {
            switch (e.style().keyword(Prop.DISPLAY)) {
                case "stack" -> stack.arrange(e);
                case "grid" -> grid.arrange(e);
                default -> flex.arrange(e);
            }
        }
        positioned.arrangeAll(e);
        if (Flow.scrolls(e)) e.scrollTo(e.scrollX(), e.scrollY());
    }

    public void place(Element e, float x, float y, float w, float h) {
        arrange(e, x, y, w, h);
    }

    void extend(LayoutBox parent, float right, float bottom) {
        float cw = right - parent.innerX();
        float ch = bottom - parent.innerY();
        if (cw > parent.contentWidth) parent.contentWidth = cw;
        if (ch > parent.contentHeight) parent.contentHeight = ch;
    }
}
