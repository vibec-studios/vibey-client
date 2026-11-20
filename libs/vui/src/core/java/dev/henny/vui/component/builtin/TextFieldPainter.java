package dev.henny.vui.component.builtin;

import dev.henny.vui.anim.Interpolate;
import dev.henny.vui.component.Controls;
import dev.henny.vui.component.text.TextEditState;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.TextMetrics;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.StateFlags;

final class TextFieldPainter {
    private static final StringBuilder MASKED = new StringBuilder();

    private TextFieldPainter() {
    }

    static void paint(Element e, TextEditState s, PaintContext ctx, int accent) {
        LayoutBox b = e.box();
        Canvas c = ctx.canvas();
        TextMetrics m = ctx.metrics();
        boolean focused = e.has(StateFlags.FOCUS);
        TextFieldGeometry.keepCaretVisible(e, s, m);
        float x = b.innerX();
        float y = b.innerY() + (b.innerHeight() - m.lineHeight()) / 2f + 1;
        c.pushScissor(b.innerX(), b.innerY(), b.innerWidth(), b.innerHeight());
        if (s.length() == 0 && !focused) {
            String placeholder = Controls.string(e, "placeholder", "");
            if (!placeholder.isEmpty()) c.text(placeholder, 0, placeholder.length(), x, y, ctx.token("dim", 0xFF6B6B75), false, false);
        }
        float origin = x - s.scroll;
        if (focused && s.hasSelection()) {
            float sx = TextFieldGeometry.widthTo(e, s, s.selectionStart(), m);
            float ex = TextFieldGeometry.widthTo(e, s, s.selectionEnd(), m);
            c.fill(origin + sx, y - 1, ex - sx, m.lineHeight(), Interpolate.withAlpha(accent, 0.45f));
        }
        CharSequence shown = TextFieldGeometry.masked(e) ? mask(s.length()) : s.chars();
        c.text(shown, 0, shown.length(), origin, y, e.style().color(Prop.COLOR), false, false);
        if (focused && (s.blink % 1f) < 0.55f) {
            float cx = origin + TextFieldGeometry.widthTo(e, s, s.caret(), m);
            c.fill(cx, y - 1, 1, m.lineHeight(), accent);
        }
        c.popScissor();
    }

    private static CharSequence mask(int length) {
        MASKED.setLength(0);
        for (int i = 0; i < length; i++) MASKED.append(TextFieldGeometry.MASK);
        return MASKED;
    }
}
