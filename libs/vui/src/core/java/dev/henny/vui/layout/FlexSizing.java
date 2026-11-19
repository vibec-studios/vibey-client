package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

final class FlexSizing {
    private static final int ROUNDS = 4;

    private FlexSizing() {
    }

    static void distribute(Element e, int from, int to, boolean row, float mainSize, float gap) {
        if (!Float.isFinite(mainSize)) return;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (Flow.inFlow(c)) c.box().flexFrozen = false;
        }
        for (int round = 0; round < ROUNDS; round++) {
            float free = mainSize - used(e, from, to, row, gap);
            if (Math.abs(free) < 0.01f) return;
            if (!(free > 0 ? grow(e, from, to, row, free, mainSize) : shrink(e, from, to, row, free, mainSize))) return;
        }
    }

    private static float used(Element e, int from, int to, boolean row, float gap) {
        float used = 0;
        int count = 0;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            LayoutBox cb = c.box();
            used += cb.flexMain + (row ? cb.marginLeft + cb.marginRight : cb.marginTop + cb.marginBottom);
            count++;
        }
        return used + gap * Math.max(0, count - 1);
    }

    private static boolean grow(Element e, int from, int to, boolean row, float free, float mainSize) {
        float total = 0;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (Flow.inFlow(c) && !c.box().flexFrozen) total += c.style().number(Prop.GROW);
        }
        if (total <= 0) return false;
        boolean clamped = false;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c) || c.box().flexFrozen) continue;
            float g = c.style().number(Prop.GROW);
            if (g <= 0) continue;
            clamped |= apply(c, row, c.box().flexMain + free * g / total, mainSize);
        }
        return clamped;
    }

    private static boolean shrink(Element e, int from, int to, boolean row, float free, float mainSize) {
        float total = 0;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (Flow.inFlow(c) && !c.box().flexFrozen) total += c.style().number(Prop.SHRINK) * c.box().flexBasis;
        }
        if (total <= 0) return false;
        boolean clamped = false;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c) || c.box().flexFrozen) continue;
            float weight = c.style().number(Prop.SHRINK) * c.box().flexBasis;
            if (weight <= 0) continue;
            clamped |= apply(c, row, c.box().flexMain + free * weight / total, mainSize);
        }
        return clamped;
    }

    private static boolean apply(Element c, boolean row, float target, float mainSize) {
        ComputedStyle s = c.style();
        float min = Lengths.orZero(s, row ? Prop.MIN_WIDTH : Prop.MIN_HEIGHT, mainSize);
        float max = Lengths.max(s, row ? Prop.MAX_WIDTH : Prop.MAX_HEIGHT, mainSize);
        float v = Lengths.clamp(target, Math.max(0, min), max);
        c.box().flexMain = v;
        if (v != target) {
            c.box().flexFrozen = true;
            return true;
        }
        return false;
    }
}
