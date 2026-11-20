package dev.henny.vui.mc.dev;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.mc.draw.McCanvas;
import dev.henny.vui.mc.input.ModState;
import dev.henny.vui.render.TextMetrics;

import java.util.List;
import java.util.function.Supplier;

public final class Inspector {
    private static final int KEY_I = 'I';

    private final McCanvas canvas;
    private final TextMetrics metrics;
    private final Supplier<List<Document>> candidates;
    private final boolean enabled;
    private boolean on;

    public Inspector(McCanvas canvas, TextMetrics metrics, Supplier<List<Document>> candidates, boolean enabled) {
        this.canvas = canvas;
        this.metrics = metrics;
        this.candidates = candidates;
        this.enabled = enabled;
    }

    public boolean on() {
        return on;
    }

    public void set(boolean value) {
        on = enabled && value;
    }

    public void toggle() {
        set(!on);
    }

    public boolean key(int key) {
        if (!enabled || key != KEY_I || !ModState.control() || !ModState.shift()) return false;
        toggle();
        return true;
    }

    public Element pick(Document doc, float x, float y) {
        doc.layoutIfNeeded();
        return doc.events().hit(x, y);
    }

    public void draw(Object graphics, int mouseX, int mouseY) {
        if (!on) return;
        Element hit = null;
        Document owner = null;
        for (Document doc : candidates.get()) {
            Element e = pick(doc, mouseX, mouseY);
            if (e != null && e != doc.root()) {
                hit = e;
                owner = doc;
                break;
            }
        }
        canvas.begin(graphics);
        try {
            canvas.layer();
            InspectorPainter.paint(canvas, metrics, owner, hit, mouseX, mouseY, Mc.guiWidth(), Mc.guiHeight());
        } finally {
            canvas.end();
        }
    }
}
