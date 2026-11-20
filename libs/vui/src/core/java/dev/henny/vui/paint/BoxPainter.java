package dev.henny.vui.paint;

import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.TextureInfo;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

public final class BoxPainter {
    private BoxPainter() {
    }

    public static void background(Element e, PaintContext ctx) {
        ComputedStyle s = e.style();
        LayoutBox b = e.box();
        Canvas c = ctx.canvas();
        int bg = s.color(Prop.BACKGROUND);
        int to = s.color(Prop.GRADIENT_TO);
        if ((to >>> 24) != 0) c.gradient(b.x, b.y, b.width, b.height, bg, to, s.is(Prop.GRADIENT_DIRECTION, "horizontal"));
        else if ((bg >>> 24) != 0) c.fill(b.x, b.y, b.width, b.height, bg);
        Object image = s.object(Prop.BACKGROUND_IMAGE);
        if (image instanceof String id && !id.isEmpty()) image(e, id, ctx);
    }

    private static void image(Element e, String id, PaintContext ctx) {
        LayoutBox b = e.box();
        int slice = (int) e.style().number(Prop.BACKGROUND_SLICE);
        if (slice > 0) {
            TextureInfo info = ctx.textures().info(id);
            int tw = info == null ? slice * 3 : info.width();
            int th = info == null ? slice * 3 : info.height();
            ctx.canvas().nineSlice(id, b.x, b.y, b.width, b.height, slice, tw, th, 0xFFFFFFFF);
        } else {
            ctx.canvas().texture(id, b.x, b.y, b.width, b.height, 0, 0, 1, 1, 0xFFFFFFFF);
        }
    }

    public static void border(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        ComputedStyle s = e.style();
        Canvas c = ctx.canvas();
        float innerH = b.height - b.borderTop - b.borderBottom;
        side(c, b.borderTop, s.color(Prop.BORDER_TOP_COLOR), b.x, b.y, b.width, b.borderTop);
        side(c, b.borderBottom, s.color(Prop.BORDER_BOTTOM_COLOR), b.x, b.y + b.height - b.borderBottom, b.width, b.borderBottom);
        side(c, b.borderLeft, s.color(Prop.BORDER_LEFT_COLOR), b.x, b.y + b.borderTop, b.borderLeft, innerH);
        side(c, b.borderRight, s.color(Prop.BORDER_RIGHT_COLOR), b.x + b.width - b.borderRight, b.y + b.borderTop, b.borderRight, innerH);
    }

    private static void side(Canvas c, float width, int color, float x, float y, float w, float h) {
        if (width > 0 && (color >>> 24) != 0) c.fill(x, y, w, h, color);
    }
}
