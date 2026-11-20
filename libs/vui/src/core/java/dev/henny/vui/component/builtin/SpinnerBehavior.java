package dev.henny.vui.component.builtin;

import dev.henny.vui.anim.Interpolate;
import dev.henny.vui.component.Behavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.style.Prop;

public final class SpinnerBehavior implements Behavior {
    private static final int CELLS = 4;

    @Override
    public void activate(Element e) {
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        float cell = Math.max(1f, Math.min(b.innerWidth(), b.innerHeight()) / 2f - 0.5f);
        float x = b.innerX();
        float y = b.innerY();
        int accent = e.style().color(Prop.ACCENT);
        int active = (int) (ctx.time() * 8f) % CELLS;
        for (int i = 0; i < CELLS; i++) {
            int order = i == 2 ? 3 : i == 3 ? 2 : i;
            float alpha = order == active ? 1f : 0.25f;
            float cx = x + (i % 2) * (cell + 1);
            float cy = y + (i / 2) * (cell + 1);
            ctx.canvas().fill(cx, cy, cell, cell, Interpolate.withAlpha(accent, alpha));
        }
    }
}
