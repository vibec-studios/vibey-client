package dev.henny.vui.demo;

import dev.henny.vui.Vui;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Document;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.headless.TreeDump;
import dev.henny.vui.reload.DirectoryRoot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPageTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void demoPageHasNoErrors() throws Exception {
        Headless headless = Headless.create();
        Vui vui = headless.vui();
        vui.registry().addRoot(new DirectoryRoot(RESOURCES));
        DemoController controller = new DemoController();
        Document doc = vui.open("vui:demo", controller);
        doc.resize(640, 360);
        doc.frame(0);
        assertTrue(doc.diagnostics().isEmpty(), doc.diagnostics().format() + "\n" + TreeDump.dump(doc.root()));
        for (String id : new String[]{"sound", "fancy", "size", "name", "mode", "zoom", "volume", "save", "players", "accent", "tabs"}) {
            assertNotNull(doc.find(id), "missing #" + id);
        }
        Files.createDirectories(Path.of("build"));
        headless.screenshot(doc, 640, 360, 2, Path.of("build/vui-demo.png"));
    }

    @Test
    void toggleWritesBack() {
        Headless headless = Headless.create();
        Vui vui = headless.vui();
        vui.registry().addRoot(new DirectoryRoot(RESOURCES));
        DemoController controller = new DemoController();
        Document doc = vui.open("vui:demo", controller);
        doc.resize(640, 360);
        doc.frame(0);
        var box = dev.henny.vui.layout.ScreenRects.of(doc.find("sound"));
        doc.pointerMove(box.centerX(), box.centerY());
        doc.pointerDown(box.centerX(), box.centerY(), 0, 0);
        doc.pointerUp(box.centerX(), box.centerY(), 0, 0);
        assertEquals(false, controller.sound.peek());
    }

    @Test
    void hudWidgetsHaveNoErrors() {
        Vui vui = Headless.create().vui();
        vui.registry().addRoot(new DirectoryRoot(RESOURCES));
        for (String id : new String[]{"vui:hud/fps", "vui:hud/clock", "vui:hud/editor"}) {
            Document doc = vui.open(id, new Model().value("fps", 60).value("clock", "12:00").value("selected", "fps"));
            doc.resize(200, 50);
            doc.frame(0);
            assertTrue(doc.diagnostics().isEmpty(), id + ": " + doc.diagnostics().format());
        }
    }
}
