package dev.henny.vui.dom;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.reload.MemoryRoot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentCompositionTest {
    private static final String CARD = """
            <props>{ "title": "Untitled", "count": 0 }</props>
            <template>
              <column class="card-root">
                <text class="card-title">{{ title }} ({{ count }})</text>
                <slot name="header"><text class="fallback">no header</text></slot>
                <slot/>
                <slot name="item" :label="title + '!'"/>
                <button class="card-close" @click="$emit('close', title)">x</button>
              </column>
            </template>
            <style>.card-root { padding: 3; }</style>
            """;

    private static TestUi withCard(String page, Object controller) {
        TestUi ui = new TestUi();
        ui.vui.registry().addRoot(new MemoryRoot().put("test:card", CARD));
        ui.doc = ui.vui.fromSource("page.vui", page, controller);
        ui.doc.resize(200, 120);
        ui.doc.frame(0);
        return ui;
    }

    @Test
    void propsSlotsAndEvents() {
        Model m = new Model().value("name", "Hello").value("closed", "");
        TestUi ui = withCard("""
                <import name="card" src="test:card"/>
                <template>
                  <card id="c" :title="name" count="2" class="extra" @close="closed = $event">
                    <text id="body">body</text>
                    <template #item="{ label }"><text id="item">{{ label }}</text></template>
                  </card>
                </template>
                """, m);
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
        Element card = ui.find("c");
        assertTrue(card.hasClass("card-root") && card.hasClass("extra"));
        assertEquals("Hello (2)", ui.first(".card-title").text().chars().toString());
        assertNotNull(ui.first(".fallback"));
        assertNotNull(ui.find("body"));
        assertEquals("Hello!", ui.find("item").text().chars().toString());
        m.value("name", "Bye");
        ui.frame(0);
        assertEquals("Bye (2)", ui.first(".card-title").text().chars().toString());
        assertEquals("Bye!", ui.find("item").text().chars().toString());
        ui.click(clickable(ui, ".card-close"));
        assertEquals("Bye", m.get("closed"));
        assertEquals(3f, card.box().paddingTop);
    }

    @Test
    void namedSlotReplacesFallback() {
        TestUi ui = withCard("""
                <import name="card" src="test:card"/>
                <template><card><template #header><text id="h">H</text></template></card></template>""", null);
        assertNotNull(ui.find("h"));
        assertTrue(ui.doc.query(".fallback").isEmpty());
    }

    @Test
    void missingComponentShowsPlaceholderAndError() {
        TestUi ui = withCard("<import name=\"nope\" src=\"test:nope\"/><template><nope/></template>", null);
        assertTrue(ui.errors().contains("not found") || ui.errors().contains("could not be loaded"), ui.errors());
        assertNotNull(ui.first(".vui-missing"));
    }

    @Test
    void unknownTagSuggestsBuiltIn() {
        TestUi ui = TestUi.of("<template><buton/></template>", null);
        assertTrue(ui.errors().contains("did you mean <button>"), ui.errors());
    }

    private static String clickable(TestUi ui, String selector) {
        Element e = ui.first(selector);
        e.id("tmp-click");
        ui.frame(0);
        return "tmp-click";
    }
}
