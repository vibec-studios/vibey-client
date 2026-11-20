package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.event.KeyEvent;
import dev.henny.vui.event.Keys;
import dev.henny.vui.event.PointerEvent;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.style.Prop;

public final class SliderBehavior implements Behavior {
    private static final float THUMB = 3f;

    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void activate(Element e) {
    }

    @Override
    public void mount(Element e) {
        SliderLabel.sync(e);
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (e.flags().mounted && !name.startsWith("$")) SliderLabel.sync(e);
    }

    @Override
    public boolean arrangeChildren(Element e, LayoutEngine layout) {
        float[] reserved = e.behaviorState();
        boolean label = SliderLabel.arrange(e, layout);
        if (!label && reserved == null) return false;
        if (reserved == null) {
            reserved = new float[1];
            e.behaviorState(reserved);
        }
        reserved[0] = label ? SliderLabel.reserve(e, layout) : 0;
        return label;
    }

    private static float track(Element e) {
        float[] reserved = e.behaviorState();
        return SliderLabel.trackWidth(e, e.box().innerWidth(), reserved == null ? 0 : reserved[0]);
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e) return;
        switch (event.type()) {
            case EventNames.POINTER_DOWN, EventNames.DRAG -> {
                if (event instanceof PointerEvent p && p.button() == PointerEvent.LEFT) set(e, valueAt(e, p.x()), false);
            }
            case EventNames.POINTER_UP -> e.emit("change", Controls.value(e));
            case EventNames.KEY_DOWN -> keys(e, (KeyEvent) event);
            case EventNames.WHEEL -> {
                if (event instanceof PointerEvent p && p.dy() != 0 && e.has(dev.henny.vui.style.StateFlags.FOCUS)) {
                    set(e, current(e) + Math.signum(p.dy()) * step(e), true);
                    event.preventDefault();
                }
            }
            default -> {
            }
        }
    }

    private void keys(Element e, KeyEvent k) {
        float v = current(e);
        float big = (max(e) - min(e)) / 10f;
        switch (k.key()) {
            case Keys.LEFT, Keys.DOWN -> set(e, v - step(e), true);
            case Keys.RIGHT, Keys.UP -> set(e, v + step(e), true);
            case Keys.PAGE_DOWN -> set(e, v - big, true);
            case Keys.PAGE_UP -> set(e, v + big, true);
            case Keys.HOME -> set(e, min(e), true);
            case Keys.END -> set(e, max(e), true);
            default -> {
                return;
            }
        }
        k.preventDefault();
    }

    private void set(Element e, float raw, boolean commit) {
        float v = snap(e, raw);
        if (v == current(e)) return;
        Double boxed = (double) v;
        if (commit) Controls.commit(e, boxed);
        else Controls.input(e, boxed);
    }

    private float valueAt(Element e, float screenX) {
        return min(e) + LocalPoint.fractionX(e, screenX, track(e)) * (max(e) - min(e));
    }

    static float snap(Element e, float raw) {
        float min = min(e);
        float max = max(e);
        float step = step(e);
        float v = Math.max(min, Math.min(max, raw));
        if (step > 0) v = min + Math.round((v - min) / step) * step;
        float rounded = Math.round(v * 10000f) / 10000f;
        return Math.max(min, Math.min(max, rounded));
    }

    static float current(Element e) {
        return Controls.number(e, "value", min(e));
    }

    static float min(Element e) {
        return Controls.number(e, "min", 0f);
    }

    static float max(Element e) {
        return Controls.number(e, "max", 1f);
    }

    static float step(Element e) {
        float s = Controls.number(e, "step", 0f);
        return s > 0 ? s : (max(e) - min(e)) / 100f;
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        Canvas c = ctx.canvas();
        float range = max(e) - min(e);
        float t = range == 0 ? 0 : (current(e) - min(e)) / range;
        float x = b.innerX();
        float w = track(e);
        float cy = b.innerY() + b.innerHeight() / 2f;
        c.fill(x, cy - 1, w, 2, e.style().color(Prop.TRACK_COLOR));
        c.fill(x, cy - 1, w * t, 2, e.style().color(Prop.ACCENT));
        float tx = x + (w - THUMB) * t;
        c.fill(tx, b.innerY(), THUMB, b.innerHeight(), e.style().color(Prop.THUMB_COLOR));
    }
}
