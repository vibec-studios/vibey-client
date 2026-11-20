package dev.henny.vui.dom;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.Model;
import dev.henny.vui.bind.State;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectivesTest {
    @Test
    void ifElseIfElseChain() {
        Model m = new Model().value("n", 1);
        TestUi ui = TestUi.of("""
                <template><column>
                  <text v-if="n == 1" id="one">1</text>
                  <text v-else-if="n == 2" id="two">2</text>
                  <text v-else id="other">x</text>
                </column></template>""", m);
        assertNotNull(ui.doc.find("one"));
        assertNull(ui.doc.find("two"));
        m.value("n", 2);
        ui.frame(0);
        assertNull(ui.doc.find("one"));
        assertNotNull(ui.doc.find("two"));
        m.value("n", 9);
        ui.frame(0);
        assertNotNull(ui.doc.find("other"));
    }

    @Test
    void elseWithoutIfIsAnError() {
        TestUi ui = TestUi.of("<template><column><text v-else>x</text></column></template>", null);
        assertTrue(ui.errors().contains("v-else without a v-if"), ui.errors());
    }

    @Test
    void keyedForReusesElementsOnReorder() {
        ListState<String> items = ListState.of("a", "b", "c");
        Model m = new Model().list("items", items);
        TestUi ui = TestUi.of("<template><column id=\"c\"><text v-for=\"(it, i) in items\" :key=\"it\" :id=\"it\">{{ i }}:{{ it }}</text></column></template>", m);
        Element a = ui.find("a");
        Element c = ui.find("c");
        items.move(0, 2);
        ui.frame(0);
        assertSame(a, ui.find("a"));
        assertEquals("b", ui.find("c").child(0).id());
        assertEquals("2:a", a.text().chars().toString());
        items.remove("b");
        ui.frame(0);
        assertEquals(2, c.childCount());
        assertTrue(ui.find("a") == a);
    }

    @Test
    void forOverNumbersMapsAndTemplates() {
        Model m = new Model().value("map", new java.util.LinkedHashMap<>(java.util.Map.of("k", "v")));
        TestUi ui = TestUi.of("""
                <template><column>
                  <row id="nums"><text v-for="n in 3">{{ n }}</text></row>
                  <row id="map"><text v-for="(v, k, i) in map">{{ k }}={{ v }}@{{ i }}</text></row>
                  <row id="tpl"><template v-for="n in 2"><text>a</text><text>b</text></template></row>
                </column></template>""", m);
        assertEquals(3, ui.find("nums").childCount());
        assertEquals("k=v@0", ui.find("map").child(0).text().chars().toString());
        assertEquals(4, ui.find("tpl").childCount());
    }

    @Test
    void vShowHidesWithoutDestroying() {
        Model m = new Model().value("on", true);
        TestUi ui = TestUi.of("<template><column><text id=\"t\" v-show=\"on\">x</text></column></template>", m);
        Element t = ui.find("t");
        m.value("on", false);
        ui.frame(0);
        assertSame(t, ui.find("t"));
        assertFalse(t.flags().shown);
    }

    @Test
    void refsAreRegistered() {
        TestUi ui = TestUi.of("<template><column><textfield ref=\"name\"/></column></template>", null);
        assertEquals("textfield", ui.doc.ref("name").tag());
    }

    @Test
    void plainFieldsRefreshAfterHandlers() {
        class Ctrl {
            public int count;

            public void inc() {
                count++;
            }
        }
        Ctrl c = new Ctrl();
        TestUi ui = TestUi.of("<template><column><button id=\"b\" @click=\"inc\">+</button><text id=\"t\">{{ count }}</text></column></template>", c);
        ui.click("b").click("b");
        assertEquals("2", ui.find("t").text().chars().toString());
        c.count = 10;
        ui.doc.refresh();
        ui.frame(0);
        assertEquals("10", ui.find("t").text().chars().toString());
    }

    @Test
    void eventModifiersStopAndSelf() {
        Model m = new Model().value("outer", 0).value("inner", 0);
        TestUi ui = TestUi.of("""
                <template><column id="o" @click="outer = outer + 1">
                  <button id="stop" @click.stop="inner = inner + 1">a</button>
                  <button id="pass" @click="inner = inner + 1">b</button>
                </column></template>""", m);
        ui.click("stop");
        assertEquals(0.0, ((Number) m.get("outer")).doubleValue());
        ui.click("pass");
        assertEquals(1.0, m.get("outer"));
        assertEquals(2.0, m.get("inner"));
    }

    @Test
    void keyModifiersFilterKeys() {
        Model m = new Model().value("hits", 0);
        TestUi ui = TestUi.of("<template><column><textfield id=\"f\" @keydown.ctrl.enter=\"hits = hits + 1\"/></column></template>", m);
        ui.click("f");
        ui.doc.keyDown(dev.henny.vui.event.Keys.ENTER, 0, 0);
        ui.doc.keyDown(dev.henny.vui.event.Keys.ENTER, 0, dev.henny.vui.event.Mods.CTRL);
        assertEquals(1.0, m.get("hits"));
    }

    @Test
    void captureRunsBeforeBubble() {
        StringBuilder order = new StringBuilder();
        Model m = new Model().action("log", args -> order.append(args[0]));
        TestUi ui = TestUi.of("""
                <template><column @click.capture="log('C')" @click="log('B')"><button id="b" @click="log('T')">x</button></column></template>""", m);
        ui.click("b");
        assertEquals("CTB", order.toString());
    }

    @Test
    void reactiveStateFieldsOnPlainObjects() {
        class Item {
            public final State<String> label = new State<>("one");
        }
        Item item = new Item();
        Model m = new Model();
        m.list("items", List.of(item));
        TestUi ui = TestUi.of("<template><column><text v-for=\"it in items\" id=\"t\">{{ it.label }}</text></column></template>", m);
        item.label.set("two");
        ui.frame(0);
        assertEquals("two", ui.find("t").text().chars().toString());
    }
}
