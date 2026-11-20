package dev.henny.vui.paint;

import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;

public final class ScrollbarPainter {
    private static final float WIDTH = 2f;

    private ScrollbarPainter() {
    }

    public static void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        float viewport = b.innerHeight();
        float content = b.contentHeight;
        if (content <= viewport + 0.5f || viewport <= 0) return;
        float trackY = b.y + b.borderTop;
        float trackH = b.height - b.borderTop - b.borderBottom;
        float thumbH = Math.max(8f, trackH * viewport / content);
        float maxScroll = content - viewport;
        float thumbY = trackY + (trackH - thumbH) * (e.scrollY() / maxScroll);
        float x = b.x + b.width - b.borderRight - WIDTH;
        ctx.canvas().fill(x, thumbY, WIDTH, thumbH, ctx.token("line2", 0x29FFFFFF));
    }

    public static float thumbWidth() {
        return WIDTH;
    }
}
