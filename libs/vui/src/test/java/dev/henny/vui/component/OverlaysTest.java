package dev.henny.vui.component;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.Model;
import dev.henny.vui.component.builtin.VirtualRows;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Keys;
import dev.henny.vui.host.ToastStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlaysTest {
    @Test
    void modalTrapsFocusAndClosesOnEscape() {
        Model m = new Model().value("open", false);
        TestUi ui = TestUi.of("""
                <template><column>
                  <button id="outside">o</button>
                  <modal id="m" v-model:open="open"><column class="panel"><button id="in1">a</button><button id="in2">b</button></column></modal>
                </column></template>""", m);
        Element modal = ui.find("m");
        assertFalse(modal.flags().overlayOpen);
        m.value("open", true);
        ui.frame(0);
        assertTrue(modal.flags().overlayOpen);
        assertEquals("in1", ui.doc.events().focus().focused().id());
        ui.key(Keys.TAB).key(Keys.TAB);
        assertEquals("in1", ui.doc.events().focus().focused().id());
        assertEquals(200, modal.box().width, 0.01);
        assertSame(modal, ui.doc.events().hit(1, 1));
        ui.key(Keys.ESCAPE);
        assertEquals(false, m.get("open"));
        assertFalse(modal.flags().overlayOpen);
    }

    @Test
    void popoverPlacesBelowAnchorAndLightDismisses() {
        Model m = new Model().value("open", true);
        TestUi ui = TestUi.of("""
                <template><column><row id="anchor" size="40 10"><popover id="p" v-model:open="open"><text>menu</text></popover></row></column></template>""", m);
        Element p = ui.find("p");
        Element anchor = ui.find("anchor");
        assertTrue(p.flags().overlayOpen);
        assertEquals(anchor.box().y + anchor.box().height + 2, p.box().y, 0.01);
        ui.doc.keyDown(Keys.ESCAPE, 0, 0);
        assertEquals(false, m.get("open"));
    }

    @Test
    void virtualListBuildsOnlyVisibleRows() {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) rows.add("row " + i);
        ListState<String> items = new ListState<>(rows);
        Model m = new Model().list("rows", items);
        TestUi ui = TestUi.of("""
                <template><column><list id="l" :items="rows" item-size="10" height="50">
                  <template #default="{ item, index }"><text class="cell">{{ index }}: {{ item }}</text></template>
                </list></column></template>""", m);
        Element list = ui.find("l");
        VirtualRows v = list.behaviorState();
        ui.frame(0);
        assertTrue(v.built() <= 10, "built " + v.built());
        assertEquals(10000, list.box().contentHeight, 0.01);
        assertEquals("0: row 0", list.child(0).text().chars().toString());
        list.scrollTo(0, 5000);
        ui.frame(0);
        assertTrue(v.built() <= 10);
        Element first = ui.doc.events().hit(list.box().x + 2, list.box().y + 2);
        assertNotNull(first.text(), first + " scroll=" + list.scrollY() + " " + ui.dump());
        assertEquals("500: row 500", first.text().chars().toString(), ui.dump());
        items.set(500, "changed");
        ui.frame(0);
        assertEquals("500: changed", first.text().chars().toString());
    }

    @Test
    void toastStackShowsAndExpires() {
        TestUi ui = new TestUi();
        ToastStack toasts = new ToastStack(ui.vui);
        toasts.resize(300, 200);
        toasts.push("Saved", "All good", null, "ok", 1f);
        toasts.frame(0);
        assertTrue(toasts.document().diagnostics().isEmpty(), toasts.document().diagnostics().format());
        Element t = toasts.document().query("toast").get(0);
        assertTrue(t.box().x + t.box().width <= 294.01, t.box().toString());
        assertTrue(t.box().y + t.box().height <= 194.01);
        toasts.frame(0.6f);
        toasts.frame(0.6f);
        for (int i = 0; i < 10; i++) toasts.frame(0.05f);
        assertEquals(0, toasts.document().query("toast").size());
    }

    @Test
    void tooltipChildElementOpensOnHover() {
        TestUi ui = TestUi.of("<template><column><button id=\"b\">i<tooltip id=\"tip\"><text>rich</text></tooltip></button></column></template>", null);
        Element b = ui.find("b");
        ui.doc.pointerMove(b.box().x + 1, b.box().y + 1);
        ui.frame(0.5f);
        assertNotNull(ui.find("tip"));
        assertTrue(ui.find("tip").flags().overlayOpen);
    }
}
