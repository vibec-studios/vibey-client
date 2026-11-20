package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.event.PointerEvent;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.Canvas;

public final class ColorAreaBehavior implements Behavior {
    private static final float HUE_WIDTH = 8f;
    private static final float GAP = 3f;

    @Override
    public void activate(Element e) {
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e || !(event instanceof PointerEvent p)) return;
        if (!event.type().equals(EventNames.POINTER_DOWN) && !event.type().equals(EventNames.DRAG)) return;
        float[] hsv = hsv(e);
        LayoutBox b = e.box();
        float squareW = b.innerWidth() - HUE_WIDTH - GAP;
        float lx = p.localX() - b.borderLeft - b.paddingLeft;
        float ly = Math.max(0, Math.min(1, (p.localY() - b.borderTop - b.paddingTop) / Math.max(1, b.innerHeight())));
        boolean onHue = event.type().equals(EventNames.POINTER_DOWN) ? lx > squareW : e.prop("$dragHue") == Boolean.TRUE;
        if (event.type().equals(EventNames.POINTER_DOWN)) e.prop("$dragHue", onHue);
        if (onHue) {
            hsv[0] = ly;
        } else {
            hsv[1] = Math.max(0, Math.min(1, lx / Math.max(1, squareW)));
            hsv[2] = 1f - ly;
        }
        Element picker = ColorPickerParts.picker(e);
        if (picker != null) Controls.input(picker, ColorValue.format(Hsv.toRgb(hsv[0], hsv[1], hsv[2]), Controls.value(picker)));
        event.preventDefault();
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        float[] hsv = hsv(e);
        LayoutBox b = e.box();
        Canvas c = ctx.canvas();
        float x = b.innerX();
        float y = b.innerY();
        float h = b.innerHeight();
        float squareW = b.innerWidth() - HUE_WIDTH - GAP;
        c.gradient(x, y, squareW, h, 0xFFFFFFFF, Hsv.toRgb(hsv[0], 1, 1), true);
        c.gradient(x, y, squareW, h, 0x00000000, 0xFF000000, false);
        float hx = x + squareW + GAP;
        for (int i = 0; i < 6; i++) {
            float sy = y + h * i / 6f;
            c.gradient(hx, sy, HUE_WIDTH, h / 6f, Hsv.toRgb(i / 6f, 1, 1), Hsv.toRgb((i + 1) / 6f, 1, 1), false);
        }
        c.fill(x + hsv[1] * squareW - 1, y + (1 - hsv[2]) * h - 1, 3, 3, 0xFFFFFFFF);
        c.fill(hx - 1, y + hsv[0] * h - 0.5f, HUE_WIDTH + 2, 1, 0xFFFFFFFF);
    }

    static void reset(Element area, int argb) {
        area.behaviorState(Hsv.fromRgb(argb));
    }

    private static float[] hsv(Element e) {
        float[] hsv = e.behaviorState();
        if (hsv == null) {
            Element picker = ColorPickerParts.picker(e);
            hsv = Hsv.fromRgb(picker == null ? 0xFFFFFFFF : ColorValue.parse(Controls.value(picker)));
            e.behaviorState(hsv);
        }
        return hsv;
    }
}
