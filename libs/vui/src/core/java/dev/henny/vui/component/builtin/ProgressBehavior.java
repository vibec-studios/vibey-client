package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.style.Prop;

public final class ProgressBehavior implements Behavior {
    @Override
    public void activate(Element e) {
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        float x = b.innerX();
        float y = b.innerY();
        float w = b.innerWidth();
        float h = b.innerHeight();
        ctx.canvas().fill(x, y, w, h, e.style().color(Prop.TRACK_COLOR));
        int accent = e.style().color(Prop.ACCENT);
        if (e.prop("value") == null) {
            float segment = w * 0.3f;
            float phase = (ctx.time() * 0.8f) % 1f;
            float sx = x - segment + (w + segment) * phase;
            float left = Math.max(x, sx);
            float right = Math.min(x + w, sx + segment);
            if (right > left) ctx.canvas().fill(left, y, right - left, h, accent);
            return;
        }
        float max = Controls.number(e, "max", 1f);
        float t = max <= 0 ? 0 : Math.max(0, Math.min(1, Controls.number(e, "value", 0) / max));
        ctx.canvas().fill(x, y, w * t, h, accent);
    }
}
