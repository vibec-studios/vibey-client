package dev.henny.vibeyclient.ui.hud.render;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.Size;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.Canvas;

final class PaintBehavior implements Behavior {
    static final String TAG = "vibey-hud-paint";

    @Override
    public boolean measure(Element e, LayoutEngine layout, float maxWidth, float maxHeight, Size out) {
        ElementView v = view(e);
        out.set(v == null ? 1 : v.width(), v == null ? 1 : v.height());
        return true;
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        ElementView v = view(e);
        if (v == null || HudFrame.gfx() == null) return;
        LayoutBox b = e.box();
        Canvas c = ctx.canvas();
        c.push();
        c.translate(b.innerX(), b.innerY());
        try {
            v.paint();
        } finally {
            c.pop();
        }
    }

    private static ElementView view(Element e) {
        return e.prop("view") instanceof ElementView v ? v : null;
    }
}
