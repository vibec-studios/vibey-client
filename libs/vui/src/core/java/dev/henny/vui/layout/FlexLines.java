package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;

final class FlexLines {
    private int[] starts = new int[8];
    private int[] ends = new int[8];
    private int count;

    int count() {
        return count;
    }

    int start(int line) {
        return starts[line];
    }

    int end(int line) {
        return ends[line];
    }

    void split(Element e, boolean row, float mainSize, float gap, boolean wrap) {
        count = 0;
        int n = e.childCount();
        if (!wrap || !Float.isFinite(mainSize)) {
            add(0, n);
            return;
        }
        int lineStart = 0;
        float used = 0;
        int inLine = 0;
        for (int i = 0; i < n; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            LayoutBox cb = c.box();
            float outer = cb.flexBasis + (row ? cb.marginLeft + cb.marginRight : cb.marginTop + cb.marginBottom);
            if (inLine > 0 && used + gap + outer > mainSize) {
                add(lineStart, i);
                lineStart = i;
                used = 0;
                inLine = 0;
            }
            used += (inLine > 0 ? gap : 0) + outer;
            inLine++;
        }
        add(lineStart, n);
    }

    private void add(int from, int to) {
        if (count == starts.length) {
            starts = java.util.Arrays.copyOf(starts, count * 2);
            ends = java.util.Arrays.copyOf(ends, count * 2);
        }
        starts[count] = from;
        ends[count] = to;
        count++;
    }
}
