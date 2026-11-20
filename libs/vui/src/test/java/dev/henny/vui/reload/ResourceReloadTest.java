package dev.henny.vui.reload;

import dev.henny.vui.Vui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.style.Prop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceReloadTest {
    @Test
    void themeFilesReloadAndRestyleOpenDocuments(@TempDir Path dir) throws Exception {
        Path vui = dir.resolve("assets/demo/vui");
        Files.createDirectories(vui);
        Files.writeString(vui.resolve("page.vui"), "<template><box id=\"b\"/></template><style>#b { background: $panel; }</style>");
        Path theme = vui.resolve("theme.json");
        Files.writeString(theme, "{ \"colors\": { \"panel\": \"#111111\" } }");
        Vui ui = Headless.create().vui();
        ui.registry().addRoot(new DirectoryRoot(dir));
        ui.loadTheme("assets/demo/vui/theme.json");
        assertTrue(ui.themeDiagnostics().isEmpty(), ui.themeDiagnostics().format());
        Document doc = ui.open("demo:page", null);
        doc.frame(0);
        assertEquals(0xFF111111, doc.find("b").style().color(Prop.BACKGROUND));
        Files.writeString(theme, "{ \"colors\": { \"panel\": \"#222222\" } }");
        ui.reloader().changed(List.of(theme));
        doc.frame(0);
        Element b = doc.find("b");
        assertEquals(0xFF222222, b.style().color(Prop.BACKGROUND));
        assertEquals("#222222", ui.theme().get("panel"));
    }

    @Test
    void iconFilesReload(@TempDir Path dir) throws Exception {
        Path vui = dir.resolve("assets/demo/vui");
        Files.createDirectories(vui);
        Path icons = vui.resolve("icons.json");
        Files.writeString(icons, "{ \"icons\": { \"a\": \"#\" } }");
        Vui ui = Headless.create().vui();
        ui.registry().addRoot(new DirectoryRoot(dir));
        assertNotNull(ui.icons().find("demo:a"));
        Files.writeString(icons, "{ \"icons\": { \"b\": \"#\" } }");
        ui.reloader().changed(List.of(icons));
        assertNotNull(ui.icons().find("demo:b"));
    }
}
