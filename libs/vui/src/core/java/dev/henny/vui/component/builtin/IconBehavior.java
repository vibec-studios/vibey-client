package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.style.Prop;

public final class IconBehavior implements Behavior {
    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        int color = e.style().color(Prop.COLOR);
        Object src = e.prop("src");
        if (src instanceof String id && !id.isEmpty()) {
            ctx.canvas().texture(id, b.innerX(), b.innerY(), b.innerWidth(), b.innerHeight(), 0, 0, 1, 1, color);
            return;
        }
        Object glyph = e.prop("glyph");
        if (glyph instanceof String g && !g.isEmpty()) {
            int cp = g.codePointAt(0);
            float w = ctx.metrics().glyphWidth(cp, false);
            float x = b.innerX() + (b.innerWidth() - w) / 2f;
            float y = b.innerY() + (b.innerHeight() - ctx.metrics().lineHeight()) / 2f + 1;
            ctx.canvas().glyph(cp, x, y, color, false, false);
        }
    }
}
