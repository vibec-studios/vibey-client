package dev.henny.vui.reload;

import dev.henny.vui.Vui;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.Headless;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotReloadTest {
    private static final String V1 = """
            <template><column><text id="t">v1 {{ count }}</text>
              <scroll id="s" height="20"><box v-for="i in 10" :key="i" height="10"/></scroll>
              <textfield id="f"/></column></template>""";
    private static final String V2 = """
            <template><column><text id="t">v2 {{ count }}</text>
              <scroll id="s" height="20"><box v-for="i in 10" :key="i" height="10"/></scroll>
              <textfield id="f"/></column></template>""";

    @Test
    void idsMapToResourcePaths() {
        DocumentId id = DocumentId.parse("mymod:menus/settings");
        assertEquals("assets/mymod/vui/menus/settings.vui", id.resource(DocumentId.MARKUP));
        assertEquals(id, DocumentId.fromResource("assets/mymod/vui/menus/settings.vui.json"));
    }

    @Test
    void swapKeepsControllerStateScrollAndFocus(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("assets/test/vui/page.vui");
        Files.createDirectories(file.getParent());
        Files.writeString(file, V1);
        Vui vui = Headless.create().vui();
        vui.registry().addRoot(new DirectoryRoot(dir));
        Model model = new Model().value("count", 5);
        Document doc = vui.open("test:page", model);
        doc.resize(200, 100);
        doc.frame(0);
        assertEquals("v1 5", doc.find("t").text().chars().toString());
        doc.find("s").scrollTo(0, 30);
        doc.events().focus().focus(doc.find("f"), true);
        Files.writeString(file, V2);
        vui.reloader().apply(Set.of("test:page"));
        assertEquals("v2 5", doc.find("t").text().chars().toString());
        assertEquals(30f, doc.find("s").scrollY(), 0.01);
        assertEquals(doc.find("f"), doc.events().focus().focused());
        model.value("count", 6);
        doc.frame(0);
        assertEquals("v2 6", doc.find("t").text().chars().toString());
    }

    @Test
    void brokenEditKeepsOldTreeAndShowsErrors(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("assets/test/vui/page.vui");
        Files.createDirectories(file.getParent());
        Files.writeString(file, V1);
        Vui vui = Headless.create().vui();
        vui.registry().addRoot(new DirectoryRoot(dir));
        Document doc = vui.open("test:page", new Model().value("count", 1));
        doc.resize(200, 100);
        doc.frame(0);
        Element before = doc.find("t");
        Files.writeString(file, "<template>\n  <column>\n    <text>{{ count + }}</text>\n</template>");
        vui.reloader().apply(Set.of("test:page"));
        assertEquals(before, doc.find("t"));
        String errors = doc.diagnostics().format();
        assertTrue(errors.contains("page.vui:3:"), errors);
        dev.henny.vui.headless.ImageCanvas canvas = new dev.henny.vui.headless.ImageCanvas(200, 100, 1, null, 0xFF000000);
        doc.render(canvas);
        assertEquals(0xFFFF5470, canvas.pixel(4, 4) | 0xFF000000);
        Files.writeString(file, V2);
        vui.reloader().apply(Set.of("test:page"));
        assertTrue(doc.diagnostics().isEmpty(), doc.diagnostics().format());
    }

    @Test
    void watcherPicksUpFileChanges(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("assets/test/vui/page.vui");
        Files.createDirectories(file.getParent());
        Files.writeString(file, V1);
        Vui vui = Headless.create().vui().devMode(true);
        vui.registry().addRoot(new DirectoryRoot(dir));
        Document doc = vui.open("test:page", new Model().value("count", 1));
        doc.resize(200, 100);
        doc.frame(0);
        vui.reloader().watch();
        try {
            Thread.sleep(200);
            Files.writeString(file, V2);
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline && !doc.find("t").text().chars().toString().startsWith("v2")) {
                vui.tick();
                Thread.sleep(50);
            }
            assertEquals("v2 1", doc.find("t").text().chars().toString());
        } finally {
            vui.reloader().close();
        }
    }

    @Test
    void overridesWinOverNormalRoots(@TempDir Path dir) {
        Vui vui = Headless.create().vui();
        vui.registry().addRoot(new MemoryRoot().put("test:a", "<template><text id=\"t\">base</text></template>"));
        vui.registry().addOverride(new MemoryRoot().put("test:a", "<template><text id=\"t\">dev</text></template>"));
        Document doc = vui.open("test:a", null);
        doc.resize(100, 50);
        doc.frame(0);
        assertNotNull(doc.find("t"));
        assertEquals("dev", doc.find("t").text().chars().toString());
    }

    @Test
    void missingDocumentGivesDiagnosticNotCrash() {
        Vui vui = Headless.create().vui();
        Document doc = vui.open("test:none", null);
        doc.resize(10, 10);
        doc.frame(0);
        assertTrue(doc.diagnostics().format().contains("document 'test:none' not found"));
    }
}
