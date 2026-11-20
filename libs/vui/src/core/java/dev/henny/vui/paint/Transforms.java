package dev.henny.vui.paint;

import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

public final class Transforms {
    private Transforms() {
    }

    public static boolean active(ComputedStyle s) {
        return s.number(Prop.TRANSLATE_X) != 0 || s.number(Prop.TRANSLATE_Y) != 0 || s.number(Prop.SCALE) != 1f
                || s.number(Prop.ROTATE) != 0;
    }

    public static void push(Canvas c, Element e) {
        ComputedStyle s = e.style();
        LayoutBox b = e.box();
        float cx = b.x + b.width / 2f;
        float cy = b.y + b.height / 2f;
        c.push();
        c.translate(cx + s.number(Prop.TRANSLATE_X), cy + s.number(Prop.TRANSLATE_Y));
        float rotate = s.number(Prop.ROTATE);
        if (rotate != 0) c.rotate((float) Math.toRadians(rotate));
        float scale = s.number(Prop.SCALE);
        if (scale != 1f) c.scale(scale, scale);
        c.translate(-cx, -cy);
    }

    public static float inverseX(Element e, float x, float y) {
        ComputedStyle s = e.style();
        if (!active(s)) return x;
        LayoutBox b = e.box();
        float cx = b.x + b.width / 2f;
        float cy = b.y + b.height / 2f;
        float dx = x - cx - s.number(Prop.TRANSLATE_X);
        float dy = y - cy - s.number(Prop.TRANSLATE_Y);
        double r = -Math.toRadians(s.number(Prop.ROTATE));
        float rx = (float) (dx * Math.cos(r) - dy * Math.sin(r));
        float scale = s.number(Prop.SCALE) == 0 ? 1 : s.number(Prop.SCALE);
        return cx + rx / scale;
    }

    public static float inverseY(Element e, float x, float y) {
        ComputedStyle s = e.style();
        if (!active(s)) return y;
        LayoutBox b = e.box();
        float cx = b.x + b.width / 2f;
        float cy = b.y + b.height / 2f;
        float dx = x - cx - s.number(Prop.TRANSLATE_X);
        float dy = y - cy - s.number(Prop.TRANSLATE_Y);
        double r = -Math.toRadians(s.number(Prop.ROTATE));
        float ry = (float) (dx * Math.sin(r) + dy * Math.cos(r));
        float scale = s.number(Prop.SCALE) == 0 ? 1 : s.number(Prop.SCALE);
        return cy + ry / scale;
    }
}
