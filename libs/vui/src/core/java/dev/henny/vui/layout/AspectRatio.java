package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.Prop;

final class AspectRatio {
    private AspectRatio() {
    }

    static float of(Element e) {
        return e.style().number(Prop.ASPECT_RATIO);
    }

    static boolean has(Element e) {
        return of(e) > 0;
    }

    static float width(float ratio, float w, float h, float available) {
        if (!Float.isNaN(w)) return w;
        if (!Float.isNaN(h)) return h * ratio;
        return Float.isFinite(available) ? available : Float.NaN;
    }

    static float height(float ratio, float w, float h) {
        if (!Float.isNaN(h)) return h;
        return Float.isNaN(w) ? Float.NaN : w / ratio;
    }
}
