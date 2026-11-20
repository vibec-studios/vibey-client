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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorIconTest {
    private static final String ICONS = """
            { "icons": {
                "gem": { "palette": { "o": "#1b1430", "a": "#ff0000", "b": "#00ff0080" },
                         "rows": [".oo.", "oaao", "oabo", ".oo."] },
                "plus": [".#.", "###", ".#."]
              },
              "aliases": { "jewel": "gem" } }
            """;

    private static Diagnostics parse(String json) {
        Diagnostics d = new Diagnostics();
        IconSetParser.parse("demo", "icons.json", json, d);
        return d;
    }

    @Test
    void parsesPaletteRowsAndAliases() {
        Diagnostics d = new Diagnostics();
        IconSet set = IconSetParser.parse("demo", "icons.json", ICONS, d);
        assertTrue(d.isEmpty(), d.format());
        PixelIcon gem = set.get("gem");
        assertFalse(gem.tinted());
        assertTrue(set.get("plus").tinted());
        assertEquals(4, gem.width());
        assertEquals(4, gem.height());
        assertEquals(0, gem.pixel(0, 0));
        assertEquals(0xFF1B1430, gem.pixel(1, 0));
        assertEquals(0xFFFF0000, gem.pixel(1, 1));
        assertEquals(0x8000FF00, gem.pixel(2, 2));
        assertEquals(gem, set.get("jewel"));
    }

    @Test
    void runsSplitWhereTheColourChanges() {
        IconSet set = IconSetParser.parse("demo", "icons.json", ICONS, new Diagnostics());
        PixelIcon gem = set.get("gem");
        int row2 = 0;
        for (int i = 0; i < gem.runCount(); i++) if (gem.runY(i) == 2) row2++;
        assertEquals(4, row2);
        assertEquals(0xFF1B1430, gem.runColor(0));
        assertEquals(2, gem.runLength(0));
    }

    @Test
    void reportsUnknownPaletteKey() {
        Diagnostics d = parse("{ \"icons\": { \"x\": { \"palette\": { \"a\": \"#fff\" }, \"rows\": [\"a.\", \"az\"] } } }");
        assertTrue(d.format().contains("row 2 uses 'z'"), d.format());
    }

    @Test
    void reportsBadPaletteEntries() {
        assertTrue(parse("{ \"icons\": { \"x\": { \"palette\": { \"a\": \"#ggg\" }, \"rows\": [\"a\"] } } }").format().contains("palette 'a'"));
        assertTrue(parse("{ \"icons\": { \"x\": { \"palette\": { \"ab\": \"#fff\" }, \"rows\": [\"a\"] } } }").format().contains("one character"));
        assertTrue(parse("{ \"icons\": { \"x\": { \"palette\": { \".\": \"#fff\" }, \"rows\": [\"a\"] } } }").format().contains("one character"));
        assertTrue(parse("{ \"icons\": { \"x\": { \"rows\": [\"a\"] } } }").format().contains("needs a \"palette\""));
        assertTrue(parse("{ \"icons\": { \"x\": { \"palette\": { \"a\": \"#fff\" } } } }").format().contains("needs \"rows\""));
        assertTrue(parse("{ \"icons\": { \"x\": { \"palette\": { \"a\": \"#fff\" }, \"rows\": [\"aa\", \"a\"] } } }").format().contains("2 characters"));
    }

    @Test
    void badIconIsSkipped() {
        IconSet set = IconSetParser.parse("demo", "icons.json", "{ \"icons\": { \"x\": { \"palette\": {}, \"rows\": [\"a\"] } } }", new Diagnostics());
        assertNull(set.get("x"));
    }

    @Test
    void iconTagDrawsOwnColoursAndIgnoresCssColour() {
        TestUi ui = new TestUi();
        ui.vui.registry().addRoot(new MemoryRoot().putFile("assets/demo/vui/icons.json", ICONS));
        ui.doc = ui.vui.fromSource("t.vui", """
                <template><row><icon id="i" name="demo:gem" style="size: 8; color: #0000ff"/></row></template>
                """, null);
        ui.doc.resize(40, 20);
        ui.doc.frame(0);
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
        Element i = ui.find("i");
        ImageCanvas c = ui.render();
        int x = (int) i.box().x;
        int y = (int) i.box().y;
        assertEquals(0xFFFF0000, c.pixel(x + 2, y + 3));
        assertEquals(0xFF1B1430, c.pixel(x + 3, y + 1));
        assertEquals(0xFF000000, c.pixel(x, y));
    }

    @Test
    void cssAlphaFadesColouredIcons() {
        assertEquals(0x80FF0000, IconFills.fade(0xFFFF0000, 0x80));
        assertEquals(0xFFFF0000, IconFills.fade(0xFFFF0000, 0xFF));
    }

    @Test
    void atlasKeepsColours() {
        IconSet set = IconSetParser.parse("demo", "icons.json", ICONS, new Diagnostics());
        PixelIcon gem = set.get("gem");
        IconAtlas atlas = IconAtlas.build(List.copyOf(set.icons()), 32);
        int px = Math.round(atlas.u0(gem) * atlas.width());
        int py = Math.round(atlas.v0(gem) * atlas.height());
        assertEquals(0xFFFF0000, atlas.pixels()[(py + 1) * atlas.width() + px + 1]);
    }
}
