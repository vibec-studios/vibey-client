package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Controls;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.Size;
import dev.henny.vui.style.StateFlags;

final class SelectSegments {
    static final String SEGMENT = "segment";
    static final String STATE = "$segmented";
    private static final Size SCRATCH = new Size();

    private SelectSegments() {
    }

    static boolean enabled(Element select) {
        String mode = Controls.string(select, "mode", "menu");
        return mode.equals("auto") || mode.equals("segmented");
    }

    static boolean showing(Element select) {
        return select.prop(STATE) == Boolean.TRUE;
    }

    static void rebuild(Element select) {
        Internal.clear(select, SEGMENT);
        if (!enabled(select)) {
            select.prop(STATE, false);
            return;
        }
        for (Controls.Option o : Controls.options(select)) {
            Element seg = Internal.child(select, SEGMENT);
            seg.prop("value", o.value());
            Internal.label(seg, o.label());
            seg.flags().shown = false;
        }
        mark(select);
    }

    static void mark(Element select) {
        Object value = Controls.value(select);
        for (int i = 0; i < select.childCount(); i++) {
            Element c = select.child(i);
            if (c.flags().internal && c.tag().equals(SEGMENT)) c.state(StateFlags.SELECTED, Values.looseEquals(c.prop("value"), value));
        }
    }

    static boolean measure(Element select, LayoutEngine layout, float maxWidth, float maxHeight, Size out) {
        if (!enabled(select)) return false;
        LayoutBox b = select.box();
        float segments = segmentsWidth(select, layout, maxHeight, out);
        float segHeight = out.height;
        float outer = segments + b.borderLeft + b.borderRight;
        boolean fits = forced(select) || outer <= maxWidth + b.horizontalExtras() + 0.01f;
        if (fits) {
            out.set(Math.max(0, outer - b.horizontalExtras()), Math.max(0, segHeight - b.paddingTop - b.paddingBottom));
            return true;
        }
        Element label = Internal.find(select, "text");
        if (label == null) return false;
        layout.measure(label, maxWidth, maxHeight);
        out.set(label.box().measuredWidth, label.box().measuredHeight);
        return true;
    }

    static boolean arrange(Element select, LayoutEngine layout) {
        if (!enabled(select)) return false;
        LayoutBox b = select.box();
        float segments = segmentsWidth(select, layout, b.height, SCRATCH);
        boolean fits = forced(select) || segments + b.borderLeft + b.borderRight <= b.width + 0.01f;
        select.prop(STATE, fits);
        Element label = Internal.find(select, "text");
        if (label != null) label.flags().shown = !fits;
        int count = 0;
        for (int i = 0; i < select.childCount(); i++) if (isSegment(select.child(i))) count++;
        float x = b.x + b.borderLeft;
        float y = b.y + b.borderTop;
        float h = b.height - b.borderTop - b.borderBottom;
        float extra = count == 0 ? 0 : Math.max(0, b.width - b.borderLeft - b.borderRight - segments) / count;
        for (int i = 0; i < select.childCount(); i++) {
            Element c = select.child(i);
            if (!isSegment(c)) continue;
            c.flags().shown = fits;
            if (!fits) continue;
            float w = c.box().measuredWidth + extra;
            layout.place(c, x, y, w, h);
            x += w;
        }
        if (!fits && label != null) {
            layout.measure(label, b.innerWidth(), b.innerHeight());
            float lh = label.box().measuredHeight;
            layout.place(label, b.innerX(), b.innerY() + (b.innerHeight() - lh) / 2f, b.innerWidth(), lh);
        }
        return true;
    }

    private static boolean forced(Element select) {
        return Controls.string(select, "mode", "menu").equals("segmented");
    }

    private static float segmentsWidth(Element select, LayoutEngine layout, float maxHeight, Size out) {
        float w = 0;
        float h = 0;
        for (int i = 0; i < select.childCount(); i++) {
            Element c = select.child(i);
            if (!isSegment(c)) continue;
            boolean shown = c.flags().shown;
            c.flags().shown = true;
            layout.measure(c, Float.POSITIVE_INFINITY, maxHeight);
            c.flags().shown = shown;
            w += c.box().measuredWidth;
            h = Math.max(h, c.box().measuredHeight);
        }
        out.set(w, h);
        return w;
    }

    private static boolean isSegment(Element c) {
        return c.flags().internal && c.tag().equals(SEGMENT);
    }
}
