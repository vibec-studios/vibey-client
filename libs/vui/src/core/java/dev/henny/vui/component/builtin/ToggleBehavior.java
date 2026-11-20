package dev.henny.vui.component.builtin;

import dev.henny.vui.anim.Tween;
import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.StateFlags;

public final class ToggleBehavior implements Behavior {
    private static final float DURATION = 0.15f;

    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void mount(Element e) {
        boolean on = Values.truthy(Controls.value(e));
        Tween t = new Tween(on ? 1f : 0f);
        e.behaviorState(t);
        e.state(StateFlags.CHECKED, on);
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (!name.equals("value")) return;
        boolean on = Values.truthy(value);
        e.state(StateFlags.CHECKED, on);
        Tween t = e.behaviorState();
        if (t == null || e.document() == null) return;
        t.to(on ? 1f : 0f, DURATION);
        e.document().wantsFrames(e, t.running());
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.type().equals(EventNames.CLICK) && event.currentTarget() == e) {
            Controls.commit(e, !Values.truthy(Controls.value(e)));
        }
    }

    @Override
    public void frame(Element e, float dt) {
        Tween t = e.behaviorState();
        if (t == null) return;
        t.step(dt);
        if (!t.running()) e.document().wantsFrames(e, false);
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        Tween t = e.behaviorState();
        float p = t == null ? (Values.truthy(Controls.value(e)) ? 1f : 0f) : t.value();
        float pad = 2f;
        float knob = Math.max(2f, b.innerHeight() - pad * 2);
        float travel = b.innerWidth() - knob - pad * 2;
        float x = b.innerX() + pad + travel * p;
        ctx.canvas().fill(x, b.innerY() + pad, knob, knob, e.style().color(Prop.THUMB_COLOR));
    }
}
