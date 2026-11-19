package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.Size;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.TextureInfo;
import dev.henny.vui.style.Prop;

public final class ImageBehavior implements Behavior {
    @Override
    public boolean measure(Element e, LayoutEngine layout, float maxWidth, float maxHeight, Size out) {
        TextureInfo info = info(e);
        if (info == null) return false;
        float w = info.width();
        float h = info.height();
        if (w > maxWidth && Float.isFinite(maxWidth)) {
            h = h * maxWidth / w;
            w = maxWidth;
        }
        out.set(w, h);
        return true;
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        Object src = e.prop("src");
        if (!(src instanceof String id) || id.isEmpty()) return;
        LayoutBox b = e.box();
        int tint = Controls.bool(e, "tinted") ? e.style().color(Prop.COLOR) : 0xFFFFFFFF;
        int slice = (int) Controls.number(e, "slice", 0);
        TextureInfo info = ctx.textures().info(id);
        if (slice > 0) {
            ctx.canvas().nineSlice(id, b.innerX(), b.innerY(), b.innerWidth(), b.innerHeight(), slice,
                    info == null ? slice * 3 : info.width(), info == null ? slice * 3 : info.height(), tint);
            return;
        }
        String fit = Controls.string(e, "fit", "fill");
        if (info == null || fit.equals("fill")) {
            ctx.canvas().texture(id, b.innerX(), b.innerY(), b.innerWidth(), b.innerHeight(), 0, 0, 1, 1, tint);
            return;
        }
        Fit.draw(ctx, id, info, b, fit.equals("cover"), tint);
    }

    private static TextureInfo info(Element e) {
        Object src = e.prop("src");
        if (!(src instanceof String id) || e.document() == null) return null;
        return e.document().vui().backend().textures().info(id);
    }
}
