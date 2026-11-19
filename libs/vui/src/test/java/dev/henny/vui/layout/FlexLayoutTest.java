package dev.henny.vui.layout;

import dev.henny.vui.TestUi;
import dev.henny.vui.dom.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlexLayoutTest {
    private static TestUi ui(String body, String css) {
        return TestUi.of("<template>" + body + "</template><style>" + css + "</style>", null, 200, 100);
    }

    private static void box(Element e, float x, float y, float w, float h) {
        LayoutBox b = e.box();
        assertEquals(x, b.x, 0.01, "x of " + e + " " + b);
        assertEquals(y, b.y, 0.01, "y of " + e + " " + b);
        assertEquals(w, b.width, 0.01, "w of " + e + " " + b);
        assertEquals(h, b.height, 0.01, "h of " + e + " " + b);
    }

    @Test
    void rowWithGapAndFixedSizes() {
        TestUi t = ui("<row id=\"r\" gap=\"4\" padding=\"2\"><box id=\"a\" size=\"10\"/><box id=\"b\" size=\"20 10\"/></row>", "#r { align-items: start; }");
        box(t.find("r"), 0, 0, 200, 100);
        box(t.find("a"), 2, 2, 10, 10);
        box(t.find("b"), 16, 2, 20, 10);
    }

    @Test
    void growDistributesFreeSpace() {
        TestUi t = ui("<row id=\"r\"><box id=\"a\" grow=\"1\"/><box id=\"b\" grow=\"3\"/></row>", "#r { width: 100; height: 10; align-items: stretch; }");
        box(t.find("a"), 0, 0, 25, 10);
        box(t.find("b"), 25, 0, 75, 10);
    }

    @Test
    void shrinkIsWeightedByBasisAndRespectsMin() {
        TestUi t = ui("<row id=\"r\"><box id=\"a\" width=\"100\"/><box id=\"b\" width=\"100\" min-width=\"80\"/></row>", "#r { width: 150; height: 10; }");
        assertEquals(70, t.find("a").box().width, 0.01);
        assertEquals(80, t.find("b").box().width, 0.01);
    }

    @Test
    void columnStretchesChildrenAcross() {
        TestUi t = ui("<column id=\"c\" width=\"80\"><box id=\"a\" height=\"10\"/><box id=\"b\" height=\"5\" width=\"20\"/></column>", "");
        box(t.find("a"), 0, 0, 80, 10);
        box(t.find("b"), 0, 10, 20, 5);
    }

    @Test
    void justifyAndAlignCenter() {
        TestUi t = ui("<row id=\"r\" justify=\"center\" align=\"center\"><box id=\"a\" size=\"20\"/></row>", "#r { width: 100; height: 40; }");
        box(t.find("a"), 40, 10, 20, 20);
    }

    @Test
    void spaceBetweenAndEnd() {
        TestUi t = ui("<row id=\"r\" justify=\"space-between\"><box id=\"a\" size=\"10\"/><box id=\"b\" size=\"10\"/><box id=\"c\" size=\"10\"/></row>",
                "#r { width: 100; height: 10; }");
        assertEquals(0, t.find("a").box().x, 0.01);
        assertEquals(45, t.find("b").box().x, 0.01);
        assertEquals(90, t.find("c").box().x, 0.01);
        TestUi e = ui("<row id=\"r\" justify=\"end\"><box id=\"a\" size=\"10\"/></row>", "#r { width: 100; height: 10; }");
        assertEquals(90, e.find("a").box().x, 0.01);
    }

    @Test
    void wrapCreatesLines() {
        TestUi t = ui("<row id=\"r\" wrap=\"wrap\" gap=\"2\"><box id=\"a\" size=\"40 10\"/><box id=\"b\" size=\"40 10\"/><box id=\"c\" size=\"40 10\"/></row>",
                "#r { width: 90; align-items: start; }");
        box(t.find("a"), 0, 0, 40, 10);
        box(t.find("b"), 42, 0, 40, 10);
        box(t.find("c"), 0, 12, 40, 10);
    }

    @Test
    void percentAndMargins() {
        TestUi t = ui("<row id=\"r\"><box id=\"a\" width=\"50%\" height=\"10\"/><box id=\"b\" size=\"10\"/></row>", "#r { width: 120; height: 20; align-items: start; } #b { margin: 2 3; }");
        assertEquals(60, t.find("a").box().width, 0.01);
        box(t.find("b"), 63, 2, 10, 10);
    }

    @Test
    void autoSizeShrinkWrapsContent() {
        TestUi t = ui("<stack><row id=\"r\" gap=\"2\" padding=\"1\"><box size=\"10\"/><box size=\"5 7\"/></row></stack>", "stack { align-items: start; }");
        box(t.find("r"), 0, 0, 19, 12);
    }

    @Test
    void maxWidthClamps() {
        TestUi t = ui("<column><box id=\"a\" height=\"5\" max-width=\"30\"/></column>", "");
        assertEquals(30, t.find("a").box().width, 0.01);
    }

    @Test
    void displayNoneAndVShowRemoveFromFlow() {
        TestUi t = ui("<row><box id=\"a\" size=\"10\" v-show=\"false\"/><box id=\"b\" size=\"10\"/></row>", "row { align-items: start; }");
        assertEquals(0, t.find("b").box().x, 0.01);
    }
}
