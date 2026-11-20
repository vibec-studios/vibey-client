package dev.henny.vui.style;

import dev.henny.vui.TestUi;
import dev.henny.vui.dom.Element;
import dev.henny.vui.headless.ImageCanvas;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleAdditionsTest {
    @Test
    void perSideBorderColorsAndWidths() {
        TestUi ui = TestUi.of("""
                <template><column><box id="b"/></column></template>
                <style>#b { width: 20; height: 10; border: 1 #ff0000; border-left: 2 #00ff00; border-bottom-color: #0000ff; }</style>
                """, null);
        Element b = ui.find("b");
        assertEquals(0xFFFF0000, b.style().color(Prop.BORDER_TOP_COLOR));
        assertEquals(0xFF00FF00, b.style().color(Prop.BORDER_LEFT_COLOR));
        assertEquals(0xFF0000FF, b.style().color(Prop.BORDER_BOTTOM_COLOR));
        assertEquals(2f, b.box().borderLeft);
        ImageCanvas c = ui.render();
        int x = (int) b.box().x;
        int y = (int) b.box().y;
        assertEquals(0xFFFF0000, c.pixel(x + 10, y));
        assertEquals(0xFF00FF00, c.pixel(x + 1, y + 5));
        assertEquals(0xFF0000FF, c.pixel(x + 10, y + 9));
    }

    @Test
    void borderColorShorthandTakesFourValuesAndTransitions() {
        TestUi ui = TestUi.of("<template><box id=\"b\"/></template><style>#b { border: 1; border-color: #f00 #0f0 #00f #fff; transition: border-color 100ms; }</style>", null);
        ComputedStyle s = ui.find("b").style();
        assertEquals(0xFF00FF00, s.color(Prop.BORDER_RIGHT_COLOR));
        assertEquals(0xFFFFFFFF, s.color(Prop.BORDER_LEFT_COLOR));
        assertEquals(5, Shorthands.longhandsOf("border-color").size());
    }

    @Test
    void aspectRatioSizesFromTheWidth() {
        TestUi ui = TestUi.of("""
                <template><column id="c"><box id="a"/><row><box id="r"/></row><box id="h"/></column></template>
                <style>#c { width: 80; } #a { aspect-ratio: 16/9; } #r { width: 30; aspect-ratio: 1; } #h { height: 10; aspect-ratio: 2; align-self: start; }</style>
                """, null);
        assertEquals(80f, ui.find("a").box().width);
        assertEquals(45f, ui.find("a").box().height);
        assertEquals(30f, ui.find("r").box().height);
        assertEquals(20f, ui.find("h").box().width);
    }

    @Test
    void gridColumnAndRowSpans() {
        TestUi ui = TestUi.of("""
                <template><grid id="g"><box id="wide"/><box id="tall"/><box id="a"/><box id="b"/><box id="c"/></grid></template>
                <style>#g { width: 92; grid-columns: 3; gap: 4; } box { height: 10; }
                #wide { grid-column: span 2; } #tall { grid-row: span 2; height: auto; }</style>
                """, null);
        Element wide = ui.find("wide");
        Element tall = ui.find("tall");
        assertEquals(60f, wide.box().width);
        assertEquals(64f, tall.box().x - ui.find("g").box().x);
        assertEquals(24f, tall.box().height);
        assertEquals(ui.find("a").box().y, ui.find("b").box().y);
        assertEquals(wide.box().y + 14, ui.find("a").box().y);
        assertEquals(ui.find("a").box().y + 14, ui.find("c").box().y);
    }

    @Test
    void autoMarginsTakeTheFreeSpace() {
        TestUi ui = TestUi.of("""
                <template><row id="r"><box id="a"/><box id="b"/></row><column id="c"><box id="m"/></column></template>
                <style>#r { width: 100; height: 20; } box { width: 10; height: 10; } #b { margin-left: auto; }
                #c { width: 100; height: 40; position: absolute; top: 30; } #m { margin: auto; }</style>
                """, null);
        Element r = ui.find("r");
        assertEquals(90f, ui.find("b").box().x - r.box().x);
        Element m = ui.find("m");
        Element c = ui.find("c");
        assertEquals(45f, m.box().x - c.box().x);
        assertEquals(15f, m.box().y - c.box().y);
    }

    @Test
    void siblingCombinatorsMatchAndFollowStateChanges() {
        TestUi ui = TestUi.of("""
                <template><row><box id="a" class="t" size="10"/><box id="b" class="t" size="10"/><box id="c" class="t" size="10"/></row></template>
                <style>.t + .t { background: #ff0000; } #a:hover ~ .t { color: #00ff00; } .t:first-child { padding: 1; }</style>
                """, null);
        assertEquals(0, ui.find("a").style().color(Prop.BACKGROUND) >>> 24);
        assertEquals(0xFFFF0000, ui.find("b").style().color(Prop.BACKGROUND));
        assertEquals(0xFFFF0000, ui.find("c").style().color(Prop.BACKGROUND));
        assertEquals(1f, ui.find("a").style().number(Prop.PADDING_TOP));
        assertFalse(ui.find("c").style().color(Prop.COLOR) == 0xFF00FF00);
        Element a = ui.find("a");
        ui.doc.pointerMove(a.box().x + 2, a.box().y + 2);
        ui.frame(0);
        assertEquals(0xFF00FF00, ui.find("c").style().color(Prop.COLOR));
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
    }
}
