package dev.henny.vui;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.headless.ImageCanvas;
import dev.henny.vui.headless.TreeDump;
import dev.henny.vui.template.CompiledTemplate;

public final class TestUi {
    public final Headless headless = Headless.create();
    public final Vui vui = headless.vui();
    public Document doc;

    public static TestUi of(String source, Object controller) {
        return of(source, controller, 200, 120);
    }

    public static TestUi of(String source, Object controller, int width, int height) {
        TestUi t = new TestUi();
        t.doc = t.vui.fromSource("test.vui", source, controller);
        t.doc.resize(width, height);
        t.doc.frame(0);
        return t;
    }

    public CompiledTemplate compile(String source) {
        return vui.compile("test:doc", "test.vui", source);
    }

    public TestUi frame(float dt) {
        doc.frame(dt);
        return this;
    }

    public TestUi settle() {
        for (int i = 0; i < 30; i++) doc.frame(0.05f);
        return this;
    }

    public Element find(String id) {
        Element e = doc.find(id);
        if (e == null) throw new AssertionError("no element #" + id + "\n" + dump());
        return e;
    }

    public Element first(String selector) {
        var all = doc.query(selector);
        if (all.isEmpty()) throw new AssertionError("nothing matches " + selector + "\n" + dump());
        return all.get(0);
    }

    public TestUi click(String id) {
        Element e = find(id);
        float x = e.box().x + e.box().width / 2f;
        float y = e.box().y + e.box().height / 2f;
        doc.pointerMove(x, y);
        doc.pointerDown(x, y, 0, 0);
        doc.pointerUp(x, y, 0, 0);
        doc.frame(0);
        return this;
    }

    public TestUi key(int key) {
        doc.keyDown(key, 0, 0);
        doc.keyUp(key, 0, 0);
        doc.frame(0);
        return this;
    }

    public TestUi type(String text) {
        text.codePoints().forEach(cp -> doc.charTyped(cp, 0));
        doc.frame(0);
        return this;
    }

    public String dump() {
        return TreeDump.dump(doc.root());
    }

    public ImageCanvas render() {
        ImageCanvas c = new ImageCanvas((int) doc.width(), (int) doc.height(), 1, headless.textures(), 0xFF000000);
        doc.render(c);
        return c;
    }

    public String errors() {
        return doc.diagnostics().format();
    }
}
