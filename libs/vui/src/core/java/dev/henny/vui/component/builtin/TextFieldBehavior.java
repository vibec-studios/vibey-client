package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.component.text.TextEditKeys;
import dev.henny.vui.component.text.TextEditState;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.event.KeyEvent;
import dev.henny.vui.event.PointerEvent;
import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.Size;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.Input;
import dev.henny.vui.style.Prop;

public final class TextFieldBehavior implements Behavior {
    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void activate(Element e) {
    }

    @Override
    public void mount(Element e) {
        TextEditState s = new TextEditState();
        s.setText(Values.display(Controls.value(e)));
        s.maxLength((int) Controls.number(e, "max-length", 0));
        s.moveTo(s.length(), false);
        e.behaviorState(s);
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        TextEditState s = e.behaviorState();
        if (s == null) return;
        if (name.equals("value")) s.setText(Values.display(value));
        if (name.equals("max-length")) s.maxLength((int) Values.toNumber(value));
    }

    @Override
    public boolean measure(Element e, LayoutEngine layout, float maxWidth, float maxHeight, Size out) {
        out.set(Math.min(maxWidth, 80), layout.metrics().lineHeight());
        return true;
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e) return;
        TextEditState s = e.behaviorState();
        if (s == null) return;
        switch (event.type()) {
            case EventNames.CHAR -> typed(e, s, (KeyEvent) event);
            case EventNames.KEY_DOWN -> key(e, s, (KeyEvent) event);
            case EventNames.POINTER_DOWN -> s.moveTo(TextFieldGeometry.indexAt(e, s, ((PointerEvent) event).x(), e.document().vui().backend().text()), false);
            case EventNames.DRAG -> s.moveTo(TextFieldGeometry.indexAt(e, s, ((PointerEvent) event).x(), e.document().vui().backend().text()), true);
            case EventNames.DOUBLE_CLICK -> s.selectAll();
            case EventNames.FOCUS -> e.document().wantsFrames(e, true);
            case EventNames.BLUR -> {
                e.document().wantsFrames(e, false);
                e.emit("change", s.text());
            }
            default -> {
            }
        }
    }

    private void typed(Element e, TextEditState s, KeyEvent k) {
        if (Controls.bool(e, "readonly")) return;
        if (s.insert(new String(Character.toChars(k.codepoint())))) Controls.input(e, s.text());
        k.preventDefault();
    }

    private void key(Element e, TextEditState s, KeyEvent k) {
        Input input = e.document().vui().backend().input();
        String before = s.text();
        TextEditKeys.Result r = TextEditKeys.apply(s, k, input);
        if (r == TextEditKeys.Result.NONE) return;
        k.preventDefault();
        if (r == TextEditKeys.Result.SUBMIT) {
            e.emit("submit", s.text());
            e.emit("change", s.text());
            return;
        }
        if (r == TextEditKeys.Result.EDITED && Controls.bool(e, "readonly")) {
            s.setText(before);
            return;
        }
        if (r == TextEditKeys.Result.EDITED) Controls.input(e, s.text());
    }

    @Override
    public void frame(Element e, float dt) {
        TextEditState s = e.behaviorState();
        if (s != null) s.blink += dt;
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        TextEditState s = e.behaviorState();
        if (s != null) TextFieldPainter.paint(e, s, ctx, e.style().color(Prop.ACCENT));
    }
}
