package dev.henny.vui.style;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CascadeTest {
    private static int bg(Element e) {
        return e.style().color(Prop.BACKGROUND);
    }

    @Test
    void specificityThenOrderDecides() {
        TestUi ui = TestUi.of("""
                <template><column><row id="r" class="a b"/></column></template>
                <style>
                #r { background: #00ff00; }
                .a.b { background: #0000ff; }
                row { background: #ff0000; }
                .a { background: #ffffff; }
                </style>
                """, null);
        assertEquals(0xFF00FF00, bg(ui.find("r")));
    }

    @Test
    void laterRuleWinsAtSameSpecificity() {
        TestUi ui = TestUi.of("<template><row id=\"r\" class=\"a b\"/></template><style>.a { background: #ff0000; } .b { background: #0000ff; }</style>", null);
        assertEquals(0xFF0000FF, bg(ui.find("r")));
    }

    @Test
    void inlineStyleAndStyleAttributesWin() {
        TestUi ui = TestUi.of("<template><row id=\"r\" class=\"a\" style=\"background: #123456\" gap=\"7\"/></template><style>#r { background: #ff0000; gap: 1; }</style>", null);
        Element r = ui.find("r");
        assertEquals(0xFF123456, bg(r));
        assertEquals(7f, r.style().number(Prop.COLUMN_GAP));
    }

    @Test
    void descendantAndChildCombinators() {
        TestUi ui = TestUi.of("""
                <template><column class="outer"><row><text id="deep">x</text></row><text id="direct">y</text></column></template>
                <style>.outer text { color: #ff0000; } .outer > text { color: #00ff00; }</style>
                """, null);
        assertEquals(0xFFFF0000, ui.find("deep").style().color(Prop.COLOR));
        assertEquals(0xFF00FF00, ui.find("direct").style().color(Prop.COLOR));
    }

    @Test
    void colorInheritsAndHoverRestyles() {
        TestUi ui = TestUi.of("""
                <template><column id="c" class="p"><row id="r" class="t" size="40"><text id="x">hi</text></row></column></template>
                <style>.p { color: #ff0000; } .t:hover { color: #00ff00; }</style>
                """, null);
        assertEquals(0xFFFF0000, ui.find("x").style().color(Prop.COLOR));
        Element r = ui.find("r");
        ui.doc.pointerMove(r.box().x + 5, r.box().y + 5);
        ui.frame(0);
        assertEquals(0xFF00FF00, ui.find("x").style().color(Prop.COLOR));
    }

    @Test
    void boundClassAndStyleObjects() {
        Model m = new Model().value("on", false).value("w", 30);
        TestUi ui = TestUi.of("<template><row id=\"r\" :class=\"{ active: on }\" :style=\"{ width: w }\"/></template><style>.active { background: #ffffff; }</style>", m);
        Element r = ui.find("r");
        assertEquals(0, bg(r));
        m.value("on", true).value("w", 50);
        ui.frame(0);
        assertEquals(0xFFFFFFFF, bg(r));
        assertEquals(50f, r.box().width);
    }

    @Test
    void notPseudoAndStructuralPseudos() {
        TestUi ui = TestUi.of("""
                <template><column><row id="a" class="i"/><row id="b" class="i" disabled/><row id="c" class="i"/></column></template>
                <style>.i:not(:disabled) { background: #ffffff; } .i:first-child { height: 3; } .i:last-child { height: 5; }</style>
                """, null);
        assertEquals(0xFFFFFFFF, bg(ui.find("a")));
        assertEquals(0, bg(ui.find("b")));
        assertEquals(3f, ui.find("a").box().height);
        assertEquals(5f, ui.find("c").box().height);
    }

    @Test
    void themeTokensComeFromVibeTheme() {
        TestUi ui = TestUi.of("<template><row id=\"r\" class=\"x\"/></template><style>.x { background: $brand; border: 1 $line; }</style>", null);
        assertEquals(0xFF8B6CFF, bg(ui.find("r")));
        assertEquals(0x17FFFFFF, ui.find("r").style().color(Prop.BORDER_COLOR));
    }

    @Test
    void runtimeStyleErrorsAreReportedOnce() {
        Model m = new Model().value("w", "wide");
        TestUi ui = TestUi.of("<template><row :style=\"{ width: w }\"/></template>", m);
        ui.frame(0).frame(0);
        String errors = ui.errors();
        assertEquals(1, ui.doc.diagnostics().size(), errors);
        assertTrue(errors.contains("test.vui:1:"), errors);
    }
}
