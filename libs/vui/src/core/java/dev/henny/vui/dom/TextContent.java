package dev.henny.vui.dom;

import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.GlyphWidths;
import dev.henny.vui.layout.TextLines;
import dev.henny.vui.render.TextMetrics;
import dev.henny.vui.style.Prop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TextContent implements GlyphWidths {
    private final Element owner;
    private final StringBuilder chars = new StringBuilder();
    private final List<Element> runOwners = new ArrayList<>();
    private final TextLines lines = new TextLines();
    private int[] runEnds = new int[4];
    private boolean stale = true;
    private float linesFor = Float.NaN;
    private TextMetrics metrics;

    public TextContent(Element owner) {
        this.owner = owner;
    }

    public void invalidate() {
        stale = true;
        linesFor = Float.NaN;
    }

    public CharSequence chars() {
        refresh();
        return chars;
    }

    public TextLines lines() {
        return lines;
    }

    public int runCount() {
        refresh();
        return runOwners.size();
    }

    public Element runOwner(int run) {
        return runOwners.get(run);
    }

    public int runEnd(int run) {
        return runEnds[run];
    }

    public int runAt(int index) {
        for (int r = 0; r < runOwners.size(); r++) if (index < runEnds[r]) return r;
        return Math.max(0, runOwners.size() - 1);
    }

    public boolean linesValidFor(float maxWidth) {
        return !stale && linesFor == maxWidth;
    }

    public void linesComputed(float maxWidth, TextMetrics m) {
        linesFor = maxWidth;
        metrics = m;
    }

    public void metrics(TextMetrics m) {
        metrics = m;
    }

    @Override
    public float width(int index, int codepoint) {
        Element run = runOwners.isEmpty() ? owner : runOwners.get(runAt(index));
        boolean bold = run.style().is(Prop.FONT_WEIGHT, "bold");
        return metrics.glyphWidth(codepoint, bold) + run.style().number(Prop.LETTER_SPACING);
    }

    public void refresh() {
        if (!stale) return;
        stale = false;
        chars.setLength(0);
        runOwners.clear();
        Object own = owner.prop("text");
        if (own != null) append(owner, Values.display(own));
        collect(owner);
    }

    private void collect(Element e) {
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!c.flags().shown) continue;
            if (c.tag().equals(Element.TEXT_TAG)) append(e, Values.display(c.prop("text")));
            else if (c.tag().equals("span") || c.tag().equals("b") || c.tag().equals("i")) collect(c);
        }
    }

    private void append(Element styleOwner, String s) {
        if (s.isEmpty()) return;
        String transformed = transform(styleOwner, s);
        chars.append(transformed);
        if (runOwners.size() == runEnds.length) runEnds = java.util.Arrays.copyOf(runEnds, runEnds.length * 2);
        if (!runOwners.isEmpty() && runOwners.get(runOwners.size() - 1) == styleOwner) {
            runEnds[runOwners.size() - 1] = chars.length();
            return;
        }
        runOwners.add(styleOwner);
        runEnds[runOwners.size() - 1] = chars.length();
    }

    private static String transform(Element styleOwner, String s) {
        String t = styleOwner.style().keyword(Prop.TEXT_TRANSFORM);
        if (t.equals("uppercase")) return s.toUpperCase(Locale.ROOT);
        if (t.equals("lowercase")) return s.toLowerCase(Locale.ROOT);
        return s;
    }
}
