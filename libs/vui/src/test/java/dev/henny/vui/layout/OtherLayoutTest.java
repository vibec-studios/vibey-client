package dev.henny.vui.layout;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Element;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtherLayoutTest {
    @Test
    void gridWithFixedColumns() {
        TestUi t = TestUi.of("<template><grid id=\"g\" columns=\"3\" gap=\"2\" width=\"104\"><box v-for=\"i in 5\" :id=\"'c' + i\" height=\"10\"/></grid></template>", null);
        assertEquals(0, t.find("c1").box().x, 0.01);
        assertEquals(35.33, t.find("c2").box().x, 0.01);
        assertEquals(33.33, t.find("c1").box().width, 0.01);
        assertEquals(12, t.find("c4").box().y, 0.01);
        assertEquals(22, t.find("g").box().contentHeight, 0.01);
    }

    @Test
    void gridAutoColumnsFromMinWidth() {
        TestUi t = TestUi.of("<template><grid id=\"g\" min-column=\"40\" width=\"130\"><box v-for=\"i in 4\" :id=\"'c' + i\" height=\"5\"/></grid></template>", null);
        assertEquals(43.33, t.find("c1").box().width, 0.01);
        assertEquals(5, t.find("c4").box().y, 0.01);
    }

    @Test
    void stackOverlaysAndAligns() {
        TestUi t = TestUi.of("<template><stack id=\"s\" width=\"100\" height=\"50\"><box id=\"a\"/><box id=\"b\" size=\"10\" style=\"align-self: center\"/></stack></template>", null);
        assertEquals(100, t.find("a").box().width, 0.01);
        assertEquals(45, t.find("b").box().x, 0.01);
        assertEquals(20, t.find("b").box().y, 0.01);
    }

    @Test
    void absoluteAndAnchoredPositioning() {
        TestUi t = TestUi.of("""
                <template><stack id="s" width="200" height="100">
                  <box id="abs" size="10" style="position: absolute; left: 5; top: 6"/>
                  <box id="br" size="10" style="position: absolute; anchor: bottom-right; right: 2; bottom: 3"/>
                  <box id="ctr" size="20" style="position: absolute; anchor: center"/>
                  <box id="fill" style="position: absolute; left: 10; right: 10; top: 0; height: 4"/>
                </stack></template>""", null);
        assertEquals(5, t.find("abs").box().x, 0.01);
        assertEquals(6, t.find("abs").box().y, 0.01);
        assertEquals(188, t.find("br").box().x, 0.01);
        assertEquals(87, t.find("br").box().y, 0.01);
        assertEquals(90, t.find("ctr").box().x, 0.01);
        assertEquals(40, t.find("ctr").box().y, 0.01);
        assertEquals(180, t.find("fill").box().width, 0.01);
    }

    @Test
    void anchorFromNearestPoint() {
        assertEquals(Anchor.TOP_LEFT, Anchor.nearest(1, 1, 300, 300));
        assertEquals(Anchor.CENTER, Anchor.nearest(150, 150, 300, 300));
        assertEquals(Anchor.BOTTOM_RIGHT, Anchor.nearest(290, 290, 300, 300));
    }

    @Test
    void scrollContainerMeasuresContentAndClamps() {
        TestUi t = TestUi.of("<template><scroll id=\"s\" height=\"30\" width=\"50\"><box v-for=\"i in 10\" height=\"10\"/></scroll></template>", null);
        Element s = t.find("s");
        assertEquals(100, s.box().contentHeight, 0.01);
        s.scrollTo(0, 500);
        assertEquals(70, s.scrollY(), 0.01);
        t.doc.scroll(10, 10, 0, -1, 0);
        assertEquals(70, s.scrollY(), 0.01);
        t.doc.scroll(10, 10, 0, 1, 0);
        assertTrue(s.scrollY() < 70);
    }

    @Test
    void textWrapsAndEllipsizes() {
        TestUi t = TestUi.of("""
                <template><column width="60">
                  <text id="w">hello world again</text>
                  <text id="e" class="e">hello world again</text>
                  <text id="m" style="max-lines: 1; text-overflow: ellipsis">hello world again</text>
                </column></template>
                <style>.e { text-wrap: nowrap; text-overflow: ellipsis; }</style>""", null);
        Element w = t.find("w");
        assertEquals(3, w.text().lines().count());
        assertEquals(27, w.box().height, 0.01);
        Element e = t.find("e");
        assertEquals(1, e.text().lines().count());
        assertTrue(e.text().lines().ellipsized());
        assertTrue(e.text().lines().width(0) <= 60);
        assertTrue(t.find("m").text().lines().ellipsized());
    }

    @Test
    void fontScaleScalesTextBox() {
        TestUi t = TestUi.of("<template><stack><text id=\"t\" style=\"font-scale: 2\">ab</text></stack></template><style>stack { align-items: start; }</style>", null);
        assertEquals(24, t.find("t").box().width, 0.01);
        assertEquals(18, t.find("t").box().height, 0.01);
    }

    @Test
    void layoutRunsOnlyWhenDirty() {
        TestUi t = TestUi.of("<template><column><text>{{ x }}</text></column></template>", new Model().value("x", "a"));
        int passes = t.doc.layoutEngine().passes();
        for (int i = 0; i < 10; i++) t.frame(0.016f);
        assertEquals(passes, t.doc.layoutEngine().passes());
    }

    @Test
    void textChangeTriggersRelayout() {
        Model m = new Model().value("x", "a");
        TestUi t = TestUi.of("<template><stack><text id=\"t\">{{ x }}</text></stack></template><style>stack { align-items: start; }</style>", m);
        assertEquals(6, t.find("t").box().width, 0.01);
        m.value("x", "abcd");
        t.frame(0);
        assertEquals(24, t.find("t").box().width, 0.01);
    }

    @Test
    void manyItemsInGridKeepOrder() {
        List<Integer> nums = new ArrayList<>();
        for (int i = 0; i < 12; i++) nums.add(i);
        Model m = new Model();
        m.list("nums", nums);
        TestUi t = TestUi.of("<template><grid columns=\"4\" width=\"40\"><box v-for=\"n in nums\" :id=\"'n' + n\" height=\"5\"/></grid></template>", m);
        assertEquals(5, t.find("n5").box().y, 0.01, t.dump());
        assertEquals(10, t.find("n5").box().x, 0.01, t.dump());
        assertEquals(10, t.find("n8").box().y, 0.01, t.dump());
    }
}
