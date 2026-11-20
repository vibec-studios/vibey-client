package dev.henny.vui.dom;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.reload.MemoryRoot;
import dev.henny.vui.style.Prop;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentCascadeTest {
    private static final String BUTTON = """
            <props>{ "label": "" }</props>
            <template><row class="icon-btn"><text>{{ label }}</text></row></template>
            <style>.icon-btn { background: #ff0000; padding: 1; }</style>
            """;
    private static final String ROW = """
            <import name="icon-button" src="test:button"/>
            <template><row class="pause-row"><icon-button id="b" class="pause-btn" label="x"/></row></template>
            <style>.pause-btn { background: #00ff00; }</style>
            """;
    private static final String CHIP = """
            <props>{ "item": null, "items": null }</props>
            <template><row class="chip" :class="{ on: item.on }"><text id="n">{{ item.name }} {{ items.length }}</text></row></template>
            """;

    private static TestUi page(String source, Object controller) {
        TestUi ui = new TestUi();
        ui.vui.registry().addRoot(new MemoryRoot().put("test:button", BUTTON).put("test:row", ROW).put("test:chip", CHIP));
        ui.doc = ui.vui.fromSource("page.vui", source, controller);
        ui.doc.resize(200, 120);
        ui.doc.frame(0);
        return ui;
    }

    @Test
    void importerRuleBeatsImportedComponentRuleOfEqualSpecificity() {
        TestUi ui = page("<import name=\"pause-row\" src=\"test:row\"/><template><pause-row/></template>", null);
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
        assertEquals(0xFF00FF00, ui.find("b").style().color(Prop.BACKGROUND));
    }

    @Test
    void documentRuleBeatsEveryComponentRule() {
        TestUi ui = page("""
                <import name="icon-button" src="test:button"/><import name="pause-row" src="test:row"/>
                <template><column><icon-button label="a"/><pause-row/></column></template>
                <style>.pause-btn { background: #0000ff; } .icon-btn { padding: 3; }</style>
                """, null);
        Element b = ui.find("b");
        assertEquals(0xFF0000FF, b.style().color(Prop.BACKGROUND));
        assertEquals(3f, b.style().number(Prop.PADDING_TOP));
    }

    @Test
    void objectPropsAreBoundBeforeTheFirstChildBinding() {
        Model m = new Model().value("it", Map.of("name", "Zoom", "on", true)).value("all", java.util.List.of(1, 2));
        TestUi ui = page("<import name=\"chip\" src=\"test:chip\"/><template><chip id=\"c\" :item=\"it\" :items=\"all\"/></template>", m);
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
        assertTrue(ui.find("c").hasClass("on"));
        assertEquals("Zoom 2", ui.find("n").text().chars().toString());
    }
}
