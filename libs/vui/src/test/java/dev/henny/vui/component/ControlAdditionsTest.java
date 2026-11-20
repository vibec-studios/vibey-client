package dev.henny.vui.component;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Keys;
import dev.henny.vui.style.StateFlags;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlAdditionsTest {
    private static String text(Element e) {
        return e.text().chars().toString();
    }

    @Test
    void sliderShowsItsValueWithAFormat() {
        Model m = new Model().value("v", 0.5).value("px", 4.0);
        TestUi ui = TestUi.of("""
                <template><column><slider id="a" v-model="v" format="percent"/>
                <slider id="b" v-model="px" :min="1" :max="8" :step="0.5" format="{0}×"/><slider id="c" v-model="v"/></column></template>
                """, m);
        Element a = ui.find("a");
        assertEquals("50%", text(Internal.find(a, "text")));
        assertEquals("4.0×", text(Internal.find(ui.find("b"), "text")));
        assertEquals(null, Internal.find(ui.find("c"), "text"));
        m.value("v", 0.25);
        ui.frame(0);
        assertEquals("25%", text(Internal.find(a, "text")));
        Element label = Internal.find(a, "text");
        assertTrue(label.box().x > a.box().x + a.box().width / 2, ui.dump());
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
    }

    @Test
    void colorPickerSwatchesAndHexField() {
        Model m = new Model().value("c", "#ff0000");
        TestUi ui = TestUi.of("<template><column><colorpicker id=\"p\" v-model=\"c\" :presets=\"['#3fe0c5', '#8b6cff']\"/></column></template>", m);
        ui.click("p");
        Element swatch = ui.doc.query("colorpicker-swatch").get(1);
        ui.doc.pointerDown(swatch.box().x + 2, swatch.box().y + 2, 0, 0);
        ui.doc.pointerUp(swatch.box().x + 2, swatch.box().y + 2, 0, 0);
        ui.frame(0);
        assertEquals("#8b6cff", m.get("c"));
        assertTrue(swatch.has(StateFlags.SELECTED));
        Element hex = ui.first(".colorpicker-hex");
        assertEquals("#8b6cff", hex.prop("value"));
        ui.doc.pointerDown(hex.box().x + 2, hex.box().y + 2, 0, 0);
        ui.doc.pointerUp(hex.box().x + 2, hex.box().y + 2, 0, 0);
        ui.doc.keyDown(Keys.A, 0, dev.henny.vui.event.Mods.CTRL);
        ui.type("12ab34");
        ui.key(Keys.ENTER);
        assertEquals("#12ab34", m.get("c"));
        ui.doc.keyDown(Keys.A, 0, dev.henny.vui.event.Mods.CTRL);
        ui.type("nope");
        ui.key(Keys.ENTER);
        assertTrue(hex.has(StateFlags.INVALID));
        assertEquals("#12ab34", m.get("c"));
    }

    @Test
    void selectAutoModeShowsSegmentsWhenTheyFit() {
        Model m = new Model().value("mode", "a").value("opts", List.of(java.util.Map.of("value", "a", "label", "Small"),
                java.util.Map.of("value", "b", "label", "Medium"), java.util.Map.of("value", "c", "label", "Large")));
        TestUi ui = TestUi.of("""
                <template><column><row id="wide" style="width: 200"><select id="s" v-model="mode" :options="opts" mode="auto"/></row>
                <row id="narrow" style="width: 60"><select id="t" v-model="mode" :options="opts" mode="auto"/></row></column></template>
                """, m);
        Element s = ui.find("s");
        List<Element> segs = s.children().stream().filter(c -> c.tag().equals("segment")).toList();
        assertEquals(3, segs.size());
        assertTrue(segs.get(0).flags().shown);
        assertTrue(segs.get(0).has(StateFlags.SELECTED));
        assertFalse(Internal.find(s, "text").flags().shown);
        Element b = segs.get(1);
        ui.doc.pointerDown(b.box().x + 2, b.box().y + 2, 0, 0);
        ui.doc.pointerUp(b.box().x + 2, b.box().y + 2, 0, 0);
        ui.frame(0);
        assertEquals("b", m.get("mode"));
        Element t = ui.find("t");
        assertTrue(Internal.find(t, "text").flags().shown, ui.dump());
        assertFalse(t.children().stream().anyMatch(c -> c.tag().equals("segment") && c.flags().shown));
        ui.click("t");
        assertNotNull(Internal.find(t, "select-menu"));
        assertTrue(Internal.find(t, "select-menu").flags().overlayOpen);
        assertFalse(Internal.find(s, "select-menu").flags().overlayOpen);
    }

    @Test
    void keybindCapturesMouseButtons() {
        Model m = new Model().value("key", (double) Keys.NONE);
        TestUi ui = TestUi.of("<template><column><keybind id=\"k\" v-model=\"key\"/></column></template>", m);
        Element k = ui.find("k");
        ui.click("k");
        assertTrue(k.has(StateFlags.OPEN));
        float x = k.box().x + 2;
        float y = k.box().y + 2;
        ui.doc.pointerDown(x, y, 3, 0);
        ui.doc.pointerUp(x, y, 3, 0);
        ui.frame(0);
        assertEquals((double) Keys.mouse(3), m.get("key"));
        assertFalse(k.has(StateFlags.OPEN));
        assertEquals("Mouse 4", text(k.child(0)));
        ui.click("k");
        ui.doc.pointerDown(x, y, 0, 0);
        ui.doc.pointerUp(x, y, 0, 0);
        ui.frame(0);
        assertEquals((double) Keys.mouse(0), m.get("key"));
        assertFalse(k.has(StateFlags.OPEN));
    }
}
