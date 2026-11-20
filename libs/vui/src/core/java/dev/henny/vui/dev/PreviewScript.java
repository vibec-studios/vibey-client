package dev.henny.vui.dev;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.PointerEvent;

import java.util.List;

final class PreviewScript {
    static final float FRAME = 0.05f;
    static final float SETTLE = 1.2f;

    private final Document doc;
    private float x;
    private float y;

    PreviewScript(Document doc) {
        this.doc = doc;
        this.x = doc.width() / 2f;
        this.y = doc.height() / 2f;
    }

    void run(List<PreviewStep> steps, Diagnostics problems) {
        for (PreviewStep step : steps) {
            switch (step) {
                case PreviewStep.Hover h -> hover(h.target(), problems);
                case PreviewStep.Scroll s -> {
                    doc.pointerMove(x, y);
                    doc.scroll(x, y, 0, -s.lines(), 0);
                    settle(0.3f);
                }
                case PreviewStep.Click c -> click(c.target(), problems);
                case PreviewStep.Wait w -> settle(w.seconds());
            }
        }
        settle(SETTLE);
    }

    private void hover(String target, Diagnostics problems) {
        float[] at = point(target);
        if (at == null) problems.warn(new SourcePos("<preview>", 1, 1), "--hover " + target + ": nothing matches", "use x,y, an element id or a selector");
        else hover(at[0], at[1]);
    }

    private void hover(float hx, float hy) {
        x = hx;
        y = hy;
        doc.pointerMove(x, y);
        settle(FRAME);
    }

    private void click(String target, Diagnostics problems) {
        float[] at = point(target);
        if (at == null) {
            problems.warn(new SourcePos("<preview>", 1, 1), "--click " + target + ": nothing matches", "use an element id, a selector or x,y");
            return;
        }
        hover(at[0], at[1]);
        doc.pointerDown(x, y, PointerEvent.LEFT, 0);
        doc.pointerUp(x, y, PointerEvent.LEFT, 0);
        settle(0.3f);
    }

    private float[] point(String target) {
        if (target.matches("-?[\\d.]+\\s*,\\s*-?[\\d.]+")) {
            String[] p = target.split(",");
            return new float[]{Float.parseFloat(p[0].trim()), Float.parseFloat(p[1].trim())};
        }
        doc.layoutIfNeeded();
        Element e = doc.find(target.startsWith("#") ? target.substring(1) : target);
        if (e == null) {
            List<Element> hits;
            try {
                hits = doc.query(target);
            } catch (RuntimeException bad) {
                return null;
            }
            if (hits.isEmpty()) return null;
            e = hits.get(0);
        }
        var r = dev.henny.vui.layout.ScreenRects.of(e);
        return new float[]{r.x() + r.width() / 2f, r.y() + r.height() / 2f};
    }

    private void settle(float seconds) {
        for (float t = 0; t < seconds; t += FRAME) doc.frame(FRAME);
    }
}
