package dev.henny.vui.event;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Element;
import dev.henny.vui.style.StateFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisabledHoverTest {
    private static final String PAGE = """
            <template><column>
              <row id="r" disabled="true" tooltip="Locked by the server">
                <button id="b" @click="clicks = clicks + 1">Go</button><toggle id="t" v-model="on"/>
              </row>
            </column></template>
            """;

    @Test
    void disabledControlsGetHoverAndTooltipButNoActivation() {
        Model m = new Model().value("clicks", 0.0).value("on", false);
        TestUi ui = TestUi.of(PAGE, m);
        Element b = ui.find("b");
        float x = b.box().x + 2;
        float y = b.box().y + 2;
        ui.doc.pointerMove(x, y);
        ui.frame(0);
        assertTrue(b.has(StateFlags.HOVER));
        assertTrue(ui.find("r").disabled());
        for (int i = 0; i < 10; i++) ui.frame(0.1f);
        assertTrue(ui.doc.events().tooltips().showing());
        assertSame(ui.find("r"), ui.doc.events().tooltips().owner());
        ui.click("b");
        ui.click("t");
        assertEquals(0.0, m.get("clicks"));
        assertEquals(false, m.get("on"));
        assertFalse(ui.find("t").has(StateFlags.CHECKED));
    }

    @Test
    void disabledElementShowsItsOwnTooltipAndStaysInactive() {
        TestUi ui = TestUi.of("""
                <template><column><button id="b" disabled="true" tooltip="Needs a server">Join</button>
                <button id="c" disabled="true">Host<tooltip><text>Rich</text></tooltip></button></column></template>
                """, null);
        Element b = ui.find("b");
        ui.doc.pointerMove(b.box().x + 2, b.box().y + 2);
        ui.doc.pointerDown(b.box().x + 2, b.box().y + 2, 0, 0);
        assertFalse(b.has(StateFlags.ACTIVE));
        assertTrue(ui.find("b").parent().has(StateFlags.ACTIVE));
        ui.doc.pointerUp(b.box().x + 2, b.box().y + 2, 0, 0);
        for (int i = 0; i < 10; i++) ui.frame(0.1f);
        assertSame(b, ui.doc.events().tooltips().owner());
        Element c = ui.find("c");
        ui.doc.pointerMove(c.box().x + 2, c.box().y + 2);
        for (int i = 0; i < 10; i++) ui.frame(0.1f);
        assertSame(c, ui.doc.events().tooltips().owner());
        assertTrue(ui.doc.events().tooltips().showing());
    }
}
