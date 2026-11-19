package dev.henny.vui.event;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Element;
import dev.henny.vui.style.StateFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventsTest {
    private static double num(Object o) {
        return ((Number) o).doubleValue();
    }

    private static final String FORM = """
            <template><column gap="2">
              <button id="a">A</button>
              <button id="b" disabled>B</button>
              <textfield id="c"/>
              <toggle id="d"/>
            </column></template>""";

    @Test
    void tabCyclesFocusSkippingDisabled() {
        TestUi ui = TestUi.of(FORM, null);
        FocusManager f = ui.doc.events().focus();
        ui.key(Keys.TAB);
        assertEquals("a", f.focused().id());
        ui.key(Keys.TAB);
        assertEquals("c", f.focused().id());
        ui.key(Keys.TAB);
        assertEquals("d", f.focused().id());
        ui.key(Keys.TAB);
        assertEquals("a", f.focused().id());
        ui.doc.keyDown(Keys.TAB, 0, Mods.SHIFT);
        assertEquals("d", f.focused().id());
        assertTrue(f.focused().has(StateFlags.FOCUS));
        assertTrue(f.focused().parent().has(StateFlags.FOCUS_WITHIN));
    }

    @Test
    void enterAndSpaceActivateFocused() {
        Model m = new Model().value("n", 0).value("on", false);
        TestUi ui = TestUi.of("<template><column><button id=\"a\" @click=\"n = n + 1\">A</button><toggle id=\"t\" v-model=\"on\"/></column></template>", m);
        ui.key(Keys.TAB).key(Keys.ENTER).key(Keys.SPACE);
        assertEquals(2.0, num(m.get("n")));
        ui.key(Keys.TAB).key(Keys.SPACE);
        assertEquals(true, m.get("on"));
    }

    @Test
    void escapeBlursThenAsksHost() {
        TestUi ui = TestUi.of(FORM, null);
        boolean[] closed = {false};
        ui.doc.onEscape(() -> closed[0] = true);
        ui.key(Keys.TAB);
        assertTrue(ui.doc.keyDown(Keys.ESCAPE, 0, 0));
        assertNull(ui.doc.events().focus().focused());
        assertFalse(closed[0]);
        assertTrue(ui.doc.keyDown(Keys.ESCAPE, 0, 0));
        assertTrue(closed[0]);
    }

    @Test
    void clickingDisabledDoesNothing() {
        Model m = new Model().value("n", 0);
        TestUi ui = TestUi.of("<template><column><button id=\"b\" disabled @click=\"n = 1\">B</button></column></template>", m);
        ui.click("b");
        assertEquals(0.0, num(m.get("n")));
    }

    @Test
    void hoverSetsStatesOnChain() {
        TestUi ui = TestUi.of(FORM, null);
        Element a = ui.find("a");
        ui.doc.pointerMove(a.box().x + 2, a.box().y + 2);
        assertTrue(a.has(StateFlags.HOVER));
        assertTrue(a.parent().has(StateFlags.HOVER));
        ui.doc.pointerMove(199, 119);
        assertFalse(a.has(StateFlags.HOVER));
    }

    @Test
    void doubleClickAndDragEvents() {
        Model m = new Model().value("dbl", 0).value("drags", 0).value("dx", 0);
        TestUi ui = TestUi.of("<template><column><box id=\"z\" size=\"50\" @dblclick=\"dbl = dbl + 1\" @drag=\"drags = drags + 1; dx = $event.dx\"/></column></template>", m);
        ui.doc.pointerDown(10, 10, 0, 0);
        ui.doc.pointerUp(10, 10, 0, 0);
        ui.frame(0.1f);
        ui.doc.pointerDown(10, 10, 0, 0);
        ui.doc.pointerUp(10, 10, 0, 0);
        assertEquals(1.0, num(m.get("dbl")));
        ui.doc.pointerDown(10, 10, 0, 0);
        ui.doc.pointerMove(11, 10);
        ui.doc.pointerMove(20, 12);
        ui.doc.pointerMove(30, 12);
        ui.doc.pointerUp(30, 12, 0, 0);
        assertEquals(2.0, num(m.get("drags")));
        assertEquals(20.0, num(m.get("dx")));
    }

    @Test
    void hitTestRespectsScrollAndClip() {
        TestUi ui = TestUi.of("<template><column><scroll id=\"s\" height=\"20\"><box v-for=\"i in 5\" :id=\"'i' + i\" height=\"10\"/></scroll></column></template>", null);
        Element s = ui.find("s");
        assertSame(ui.find("i1"), ui.doc.events().hit(5, 5));
        s.scrollTo(0, 20);
        assertSame(ui.find("i3"), ui.doc.events().hit(5, 5));
        assertSame(s.parent(), ui.doc.events().hit(5, 25));
    }

    @Test
    void tooltipAppearsAfterDelay() {
        TestUi ui = TestUi.of("<template><column><button id=\"b\" tooltip=\"Close the menu\">X</button></column></template>", null);
        Element b = ui.find("b");
        ui.doc.pointerMove(b.box().x + 2, b.box().y + 2);
        ui.frame(0.1f);
        assertFalse(ui.doc.events().tooltips().showing());
        ui.frame(0.5f);
        assertTrue(ui.doc.events().tooltips().showing());
        Element bubble = ui.doc.overlays().tooltipBubble();
        assertTrue(bubble.box().y >= b.box().y + b.box().height, ui.dump());
        assertEquals("Close the menu", bubble.child(0).text().chars().toString());
        ui.doc.pointerDown(b.box().x + 2, b.box().y + 2, 0, 0);
        assertFalse(ui.doc.events().tooltips().showing());
    }

    @Test
    void placementFlipsAndStaysOnScreen() {
        var anchor = new dev.henny.vui.geom.Rect(10, 110, 20, 10);
        var r = dev.henny.vui.geom.Placement.place(anchor, 50, 30, 200, 120, "bottom", "start", 2);
        assertEquals(78, r.y(), 0.01);
        var edge = dev.henny.vui.geom.Placement.place(new dev.henny.vui.geom.Rect(190, 10, 10, 10), 50, 30, 200, 120, "bottom", "start", 2);
        assertEquals(150, edge.x(), 0.01);
    }
}
