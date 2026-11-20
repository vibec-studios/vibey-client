package dev.henny.vui.mc.host;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.mc.VuiRuntime;
import dev.henny.vui.mc.draw.McCanvas;
import dev.henny.vui.mc.input.ModState;

public final class DocumentView {
    public interface Step {
        void frame(float dt);
    }

    private final Document doc;
    private final Step step;
    private final FrameClock clock = new FrameClock();

    public DocumentView(Document doc) {
        this(doc, doc::frame);
    }

    public DocumentView(Document doc, Step step) {
        this.doc = doc;
        this.step = step;
    }

    public Document document() {
        return doc;
    }

    public void draw(Object graphics, float width, float height, boolean cursor) {
        float dt = clock.tick();
        Guard.run(doc, () -> {
            doc.resize(width, height);
            step.frame(dt);
        });
        McCanvas canvas = VuiRuntime.get().canvas().begin(graphics);
        try {
            Guard.run(doc, () -> doc.render(canvas));
            if (cursor) canvas.cursor(VuiRuntime.get().input().cursor());
        } finally {
            canvas.end();
        }
    }

    public boolean hits(double x, double y) {
        return Guard.test(doc, () -> {
            doc.layoutIfNeeded();
            Element e = doc.events().hit((float) x, (float) y);
            return e != null && e != doc.root();
        });
    }

    public boolean move(double x, double y) {
        return Guard.test(doc, () -> doc.pointerMove((float) x, (float) y, ModState.current()));
    }

    public boolean down(double x, double y, int button) {
        return Guard.test(doc, () -> doc.pointerDown((float) x, (float) y, button, ModState.current()));
    }

    public boolean up(double x, double y, int button) {
        return Guard.test(doc, () -> doc.pointerUp((float) x, (float) y, button, ModState.current()));
    }

    public boolean scroll(double x, double y, double dx, double dy) {
        return Guard.test(doc, () -> doc.scroll((float) x, (float) y, (float) dx, (float) dy, ModState.current()));
    }

    public boolean keyDown(int key, int scancode) {
        return Guard.test(doc, () -> doc.keyDown(key, scancode, ModState.current()));
    }

    public boolean keyUp(int key, int scancode) {
        return Guard.test(doc, () -> doc.keyUp(key, scancode, ModState.current()));
    }

    public boolean typed(int codepoint) {
        return Guard.test(doc, () -> doc.charTyped(codepoint, ModState.current()));
    }
}
