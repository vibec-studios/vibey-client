package dev.henny.vui.paint;

import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.Flow;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

public final class ElementPainter {
    private final ClipTracker clip = new ClipTracker();
    private int transformDepth;

    public void paintTree(Element root, PaintContext ctx, float viewportW, float viewportH) {
        clip.reset(viewportW, viewportH);
        transformDepth = 0;
        paint(root, ctx, 0, 0);
    }

    public void paintOverlay(Element overlay, PaintContext ctx, float viewportW, float viewportH) {
        clip.reset(viewportW, viewportH);
        paintElement(overlay, ctx, 0, 0);
    }

    private void paint(Element e, PaintContext ctx, float offsetX, float offsetY) {
        if (!Flow.displayed(e) || e.flags().overlay) return;
        paintElement(e, ctx, offsetX, offsetY);
    }

    private void paintElement(Element e, PaintContext ctx, float offsetX, float offsetY) {
        ComputedStyle s = e.style();
        float opacity = s.number(Prop.OPACITY);
        if (opacity <= 0.001f) return;
        LayoutBox b = e.box();
        boolean transformed = Transforms.active(s);
        if (transformDepth == 0 && !transformed && !clip.intersects(b.x - offsetX, b.y - offsetY, b.width, b.height) && (Flow.clips(e) || e.childCount() == 0)) return;
        Canvas c = ctx.canvas();
        if (transformed) {
            Transforms.push(c, e);
            transformDepth++;
        }
        if (opacity < 1f) c.pushAlpha(opacity);
        if (!s.is(Prop.VISIBILITY, "hidden")) {
            BoxPainter.background(e, ctx);
            BoxPainter.border(e, ctx);
            e.behavior().paint(e, ctx);
            if (e.text() != null) TextPainter.paint(e, ctx);
        }
        paintChildren(e, ctx, offsetX, offsetY, transformed);
        if (!s.is(Prop.VISIBILITY, "hidden")) e.behavior().paintOver(e, ctx);
        if (opacity < 1f) c.popAlpha();
        if (transformed) {
            c.pop();
            transformDepth--;
        }
    }

    private void paintChildren(Element e, PaintContext ctx, float offsetX, float offsetY, boolean transformed) {
        if (e.childCount() == 0 || e.text() != null) return;
        Canvas c = ctx.canvas();
        LayoutBox b = e.box();
        boolean clips = Flow.clips(e);
        boolean scrolls = Flow.scrolls(e);
        if (clips) {
            c.pushScissor(b.x + b.borderLeft, b.y + b.borderTop, b.width - b.borderLeft - b.borderRight, b.height - b.borderTop - b.borderBottom);
            if (transformDepth == 0) clip.push(b.x + b.borderLeft - offsetX, b.y + b.borderTop - offsetY, b.width - b.borderLeft - b.borderRight, b.height - b.borderTop - b.borderBottom);
        }
        float sx = scrolls ? e.scrollX() : 0;
        float sy = scrolls ? e.scrollY() : 0;
        boolean shifted = sx != 0 || sy != 0;
        if (shifted) {
            c.push();
            c.translate(-sx, -sy);
        }
        for (int i = 0; i < e.childCount(); i++) paint(e.child(i), ctx, offsetX + sx, offsetY + sy);
        if (shifted) c.pop();
        if (clips) {
            c.popScissor();
            if (transformDepth == 0) clip.pop();
        }
        if (scrolls) ScrollbarPainter.paint(e, ctx);
    }
}
