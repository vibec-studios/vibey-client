package dev.henny.vui.icon;

import dev.henny.vui.TestUi;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.ImageCanvas;
import dev.henny.vui.reload.MemoryRoot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelIconTest {
    private static final String ICONS = """
            { "icons": {
                "plus": ["...", "###", "..."],
                "dot": "#.. ... ..."
              },
              "aliases": { "add": "plus" } }
            """;

    @Test
    void parsesRowsStringsAndAliases() {
        Diagnostics d = new Diagnostics();
        IconSet set = IconSetParser.parse("demo", "icons.json", ICONS, d);
        assertTrue(d.isEmpty(), d.format());
        PixelIcon plus = set.get("plus");
        assertEquals(3, plus.width());
        assertEquals(1, plus.runCount());
        assertEquals(3, plus.runLength(0));
        assertTrue(set.get("dot").on(0, 0));
        assertEquals(plus, set.get("add"));
    }

    @Test
    void reportsBadRows() {
        Diagnostics d = new Diagnostics();
        IconSetParser.parse("demo", "icons.json", "{ \"icons\": { \"bad\": [\"##\", \"#x\"] } }", d);
        assertTrue(d.format().contains("bad"), d.format());
    }

    @Test
    void iconTagDrawsPixelIconTintedAndCrisp() {
        TestUi ui = new TestUi();
        ui.vui.registry().addRoot(new MemoryRoot().putFile("assets/demo/vui/icons.json", ICONS));
        ui.doc = ui.vui.fromSource("t.vui", """
                <template><row><icon id="i" name="demo:plus" style="size: 9; color: #ff0000"/><icon id="j" set="demo" name="add"/></row></template>
                """, null);
        ui.doc.resize(40, 20);
        ui.doc.frame(0);
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
        Element i = ui.find("i");
        ImageCanvas c = ui.render();
        int x = (int) i.box().x;
        int y = (int) i.box().y;
        assertEquals(0xFFFF0000, c.pixel(x + 4, y + 4));
        assertEquals(0xFF000000, c.pixel(x + 4, y + 1));
        assertEquals(3f, IconFills.cell(i.document().vui().icons().find("demo:plus"), 9, 9));
    }

    @Test
    void unknownIconIsReportedWithSuggestion() {
        TestUi ui = new TestUi();
        ui.vui.registry().addRoot(new MemoryRoot().putFile("assets/demo/vui/icons.json", ICONS));
        ui.doc = ui.vui.fromSource("t.vui", "<template><icon name=\"demo:plux\"/></template>", null);
        ui.doc.frame(0);
        assertTrue(ui.errors().contains("unknown icon 'demo:plux'"), ui.errors());
        assertTrue(ui.errors().contains("demo:plus"), ui.errors());
    }

    @Test
    void registryFindsUnqualifiedNamesAndReloads() {
        MemoryRoot root = new MemoryRoot().putFile("assets/demo/vui/icons.json", ICONS);
        IconRegistry r = new IconRegistry().resources(root::read);
        assertNull(r.find("plus"));
        assertNotNull(r.find("demo:plus"));
        assertNotNull(r.find("plus"));
        int before = r.version();
        root.putFile("assets/demo/vui/icons.json", "{ \"icons\": { \"minus\": \"###\" } }");
        assertTrue(r.reload("demo"));
        assertTrue(r.version() > before);
        assertNull(r.find("demo:plus"));
        assertNotNull(r.find("demo:minus"));
    }

    @Test
    void atlasPacksIconsWithUvs() {
        IconSet set = IconSetParser.parse("demo", "icons.json", ICONS, new Diagnostics());
        IconAtlas atlas = IconAtlas.build(List.copyOf(set.icons()), 16);
        PixelIcon plus = set.get("plus");
        assertTrue(atlas.contains(plus));
        int px = Math.round(atlas.u0(plus) * atlas.width());
        int py = Math.round(atlas.v0(plus) * atlas.height());
        assertEquals(0xFFFFFFFF, atlas.pixels()[(py + 1) * atlas.width() + px]);
        assertEquals(0, atlas.pixels()[py * atlas.width() + px]);
        assertFalse(atlas.u1(plus) <= atlas.u0(plus));
    }
}
