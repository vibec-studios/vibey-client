package dev.henny.r3.vui;

import dev.henny.vui.dom.Element;

final class ScrollSum {
    private ScrollSum() {}

    static float of(Element hit) {
        float sum = 0f;
        for (Element e = hit; e != null; e = e.parent()) sum += e.scrollX() * 31f + e.scrollY();
        return sum;
    }
}
