package dev.henny.vui.component;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Keys;
import dev.henny.vui.event.Mods;
import dev.henny.vui.style.StateFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlsTest {
    private static double num(Object o) {
        return ((Number) o).doubleValue();
    }

    @Test
    void toggleAndCheckboxWriteBack() {
        Model m = new Model().value("a", false).value("b", true).value("changes", 0);
        TestUi ui = TestUi.of("""
                <template><column>
                  <toggle id="t" v-model="a" @change="changes = changes + 1"/>
                  <checkbox id="c" v-model="b" label="Show"/>
                </column></template>""", m);
        ui.click("t");
        assertEquals(true, m.get("a"));
        assertTrue(ui.find("t").has(StateFlags.CHECKED));
        assertEquals(1.0, num(m.get("changes")));
        ui.click("c");
        assertEquals(false, m.get("b"));
        assertEquals("Show", ui.find("c").child(0).text().chars().toString());
    }

    @Test
    void toggleKnobAnimatesThenStops() {
        Model m = new Model().value("a", false);
        TestUi ui = TestUi.of("<template><toggle id=\"t\" v-model=\"a\"/></template>", m);
        ui.click("t");
        dev.henny.vui.anim.Tween knob = ui.find("t").behaviorState();
        assertTrue(knob.running());
        ui.settle();
        assertFalse(knob.running());
        assertEquals(1f, knob.value());
    }

    @Test
    void sliderPointerKeysAndSnap() {
        Model m = new Model().value("v", 0.0);
        TestUi ui = TestUi.of("<template><column><slider id=\"s\" v-model=\"v\" :min=\"0\" :max=\"10\" :step=\"1\" width=\"100\"/></column></template>", m);
        Element s = ui.find("s");
        float y = s.box().y + 5;
        ui.doc.pointerDown(s.box().x + 51, y, 0, 0);
        ui.doc.pointerUp(s.box().x + 51, y, 0, 0);
        assertEquals(5.0, num(m.get("v")));
        ui.key(Keys.RIGHT);
        assertEquals(6.0, num(m.get("v")));
        ui.key(Keys.END);
        assertEquals(10.0, num(m.get("v")));
        ui.doc.pointerDown(s.box().x + 1, y, 0, 0);
        ui.doc.pointerMove(s.box().x + 30, y);
        ui.doc.pointerUp(s.box().x + 30, y, 0, 0);
        assertEquals(3.0, num(m.get("v")));
    }

    @Test
    void textFieldTypingSelectionClipboard() {
        Model m = new Model().value("name", "ab").value("submitted", "");
        TestUi ui = TestUi.of("<template><column><textfield id=\"f\" v-model=\"name\" @submit=\"submitted = $event\" max-length=\"6\"/></column></template>", m);
        ui.click("f");
        ui.type("cd");
        assertEquals("abcd", m.get("name"));
        ui.key(Keys.BACKSPACE);
        assertEquals("abc", m.get("name"));
        ui.doc.keyDown(Keys.A, 0, Mods.CTRL);
        ui.doc.keyDown(Keys.C, 0, Mods.CTRL);
        assertEquals("abc", ui.headless.input().clipboard());
        ui.type("x");
        assertEquals("x", m.get("name"));
        ui.headless.input().setClipboard("123456789");
        ui.doc.keyDown(Keys.V, 0, Mods.CTRL);
        assertEquals("x12345", m.get("name"));
        ui.key(Keys.HOME);
        ui.doc.keyDown(Keys.RIGHT, 0, Mods.SHIFT);
        ui.doc.keyDown(Keys.DELETE, 0, 0);
        assertEquals("12345", m.get("name"));
        ui.key(Keys.ENTER);
        assertEquals("12345", m.get("submitted"));
        m.value("name", "reset");
        ui.frame(0);
        dev.henny.vui.component.text.TextEditState st = ui.find("f").behaviorState();
        assertEquals("reset", st.text());
    }

    @Test
    void wordNavigation() {
        dev.henny.vui.component.text.TextEditState s = new dev.henny.vui.component.text.TextEditState();
        s.setText("hello big world");
        s.moveTo(s.length(), false);
        s.left(true, false);
        assertEquals(10, s.caret());
        s.backspace(true);
        assertEquals("hello world", s.text());
    }

    @Test
    void selectOpensMenuAndPicks() {
        Model m = new Model().value("mode", "a");
        TestUi ui = TestUi.of("<template><column><select id=\"s\" v-model=\"mode\" :options=\"[{ value: 'a', label: 'Alpha' }, { value: 'b', label: 'Beta' }]\"/></column></template>", m);
        Element s = ui.find("s");
        assertEquals("Alpha", s.child(0).text().chars().toString());
        ui.click("s");
        Element menu = ui.first("select-menu");
        assertTrue(menu.flags().overlayOpen);
        Element beta = menu.child(1);
        float x = beta.box().x + 3;
        float y = beta.box().y + 3;
        ui.doc.pointerMove(x, y);
        ui.doc.pointerDown(x, y, 0, 0);
        ui.doc.pointerUp(x, y, 0, 0);
        ui.frame(0);
        assertEquals("b", m.get("mode"));
        assertFalse(menu.flags().overlayOpen);
        assertEquals("Beta", s.child(0).text().chars().toString());
        ui.key(Keys.TAB);
        ui.key(Keys.DOWN);
        assertEquals("a", m.get("mode"));
    }

    @Test
    void segmentedAndTabs() {
        Model m = new Model().value("seg", "One").value("tab", "x");
        TestUi ui = TestUi.of("""
                <template><column>
                  <segmented id="g" v-model="seg" :options="['One', 'Two']"/>
                  <tabs id="t" v-model="tab"><tab value="x">X</tab><tab id="ty" value="y">Y</tab></tabs>
                </column></template>""", m);
        Element g = ui.find("g");
        assertTrue(g.child(0).has(StateFlags.SELECTED));
        Element two = g.child(1);
        ui.doc.pointerDown(two.box().x + 2, two.box().y + 2, 0, 0);
        ui.doc.pointerUp(two.box().x + 2, two.box().y + 2, 0, 0);
        assertEquals("Two", m.get("seg"));
        ui.click("ty");
        assertEquals("y", m.get("tab"));
        assertTrue(ui.find("ty").has(StateFlags.SELECTED));
    }

    @Test
    void keybindCapturesKeys() {
        Model m = new Model().value("key", 65);
        TestUi ui = TestUi.of("<template><column><keybind id=\"k\" v-model=\"key\"/></column></template>", m);
        Element k = ui.find("k");
        assertEquals("A", k.child(0).text().chars().toString());
        ui.click("k");
        assertTrue(k.has(StateFlags.OPEN));
        assertEquals("…", k.child(0).text().chars().toString());
        ui.key(Keys.ESCAPE);
        assertFalse(k.has(StateFlags.OPEN));
        assertEquals(65.0, num(m.get("key")));
        ui.click("k");
        ui.key(Keys.F1);
        assertEquals((double) Keys.F1, num(m.get("key")));
        assertEquals("F1", k.child(0).text().chars().toString());
    }

    @Test
    void colorPickerPanelEditsValue() {
        Model m = new Model().value("c", "#ff0000");
        TestUi ui = TestUi.of("<template><column><colorpicker id=\"p\" v-model=\"c\"/></column></template>", m);
        ui.click("p");
        Element panel = ui.first("colorpicker-panel");
        assertTrue(panel.flags().overlayOpen);
        dev.henny.vui.layout.LayoutBox b = ui.first("colorpicker-area").box();
        ui.doc.pointerDown(b.innerX() + 1, b.innerY() + b.innerHeight() - 1, 0, 0);
        ui.doc.pointerUp(b.innerX() + 1, b.innerY() + b.innerHeight() - 1, 0, 0);
        String v = (String) m.get("c");
        assertTrue(v.startsWith("#0") || v.startsWith("#1"), v);
        ui.doc.pointerDown(1, 1, 0, 0);
    }

    @Test
    void progressAndSpinnerPaint() {
        TestUi ui = TestUi.of("<template><column><progress :value=\"0.5\" width=\"100\"/><progress width=\"100\"/><spinner/></column></template>", null);
        ui.frame(0.3f);
        ui.render();
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
    }
}
