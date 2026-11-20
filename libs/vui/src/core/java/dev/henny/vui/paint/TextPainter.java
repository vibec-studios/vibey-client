package dev.henny.vui.paint;

import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.TextContent;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.TextLines;
import dev.henny.vui.layout.TextWrapper;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.Unit;
import dev.henny.vui.text.TextEffect;

public final class TextPainter {
    private TextPainter() {
    }

    public static void paint(Element e, PaintContext ctx) {
        TextContent text = e.text();
        TextLines lines = text.lines();
        if (lines.count() == 0) return;
        ComputedStyle s = e.style();
        LayoutBox b = e.box();
        float scale = s.number(Prop.FONT_SCALE) <= 0 ? 1f : s.number(Prop.FONT_SCALE);
        float lineHeight = s.unit(Prop.LINE_HEIGHT) == Unit.PX ? s.number(Prop.LINE_HEIGHT) / scale : ctx.metrics().lineHeight();
        Canvas c = ctx.canvas();
        boolean scaled = scale != 1f;
        float originX = b.innerX();
        float originY = b.innerY();
        float avail = b.innerWidth() / scale;
        if (scaled) {
            c.push();
            c.translate(originX, originY);
            c.scale(scale, scale);
            originX = 0;
            originY = 0;
        }
        String align = s.keyword(Prop.TEXT_ALIGN);
        CharSequence chars = text.chars();
        for (int l = 0; l < lines.count(); l++) {
            float lw = lines.width(l);
            float x = originX + (align.equals("center") ? (avail - lw) / 2f : align.equals("end") ? avail - lw : 0f);
            float y = originY + l * lineHeight;
            float pen = runs(text, chars, lines.start(l), lines.end(l), x, y, ctx);
            if (l == lines.count() - 1 && lines.ellipsized()) {
                Element owner = text.runOwner(Math.max(0, text.runAt(Math.max(0, lines.end(l) - 1))));
                drawRun(owner, TextWrapper.ELLIPSIS, 0, 1, x + pen, y, ctx, l);
            }
        }
        if (scaled) c.pop();
    }

    private static float runs(TextContent text, CharSequence chars, int start, int end, float x, float y, PaintContext ctx) {
        float pen = 0;
        int i = start;
        while (i < end) {
            int run = text.runAt(i);
            int runEnd = Math.min(end, text.runEnd(run));
            pen += drawRun(text.runOwner(run), chars, i, runEnd, x + pen, y, ctx, i);
            i = runEnd;
        }
        return pen;
    }

    private static float drawRun(Element owner, CharSequence chars, int start, int end, float x, float y, PaintContext ctx, int seed) {
        ComputedStyle s = owner.style();
        int color = s.color(Prop.COLOR);
        boolean shadow = s.is(Prop.TEXT_SHADOW, "on");
        boolean bold = s.is(Prop.FONT_WEIGHT, "bold");
        Object effect = s.object(Prop.TEXT_EFFECT);
        float spacing = s.number(Prop.LETTER_SPACING);
        return ctx.glyphs().draw(ctx.canvas(), ctx.metrics(), chars, start, end, x, y, color, shadow, bold,
                effect instanceof TextEffect te ? te : null, ctx.time(), spacing);
    }
}
