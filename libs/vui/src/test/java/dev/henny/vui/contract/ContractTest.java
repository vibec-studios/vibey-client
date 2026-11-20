package dev.henny.vui.contract;

import dev.henny.vui.TestUi;
import dev.henny.vui.bind.Model;
import dev.henny.vui.bind.State;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.dom.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractTest {
    private static final String DOC = """
            <contract>{
              "fields": { "title": "string", "modules": "list", "count": "number", "hint?": "string" },
              "actions": ["open(m)", "close"]
            }</contract>
            <template><column><text>{{ title }}</text></column></template>
            """;

    public static final class Controller {
        public final State<String> title = new State<>("Hi");
        public final List<String> modules = List.of("zoom");
        public double count = 2;

        public void open(Object m) {
        }

        public void close() {
        }
    }

    @Test
    void satisfiedContractIsQuiet() {
        TestUi ui = new TestUi();
        ui.vui.devMode(true);
        ui.doc = ui.vui.fromSource("c.vui", DOC, new Controller());
        ui.doc.frame(0);
        assertTrue(ui.doc.diagnostics().isEmpty(), ui.errors());
    }

    @Test
    void missingFieldsActionsAndWrongTypesAreWarnedInDevMode() {
        TestUi ui = new TestUi();
        ui.vui.devMode(true);
        Model m = new Model().value("title", 5.0);
        m.list("modules", List.of());
        ui.doc = ui.vui.fromSource("c.vui", DOC, m);
        ui.doc.frame(0);
        String text = ui.errors();
        assertTrue(text.contains("field 'title' should be string"), text);
        assertTrue(text.contains("no field 'count'"), text);
        assertFalse(text.contains("'hint'"), text);
        assertTrue(text.contains("no action 'open'"), text);
        assertTrue(text.contains("c.vui:2:"), text);
        assertEquals(0, ui.doc.diagnostics().errors().size());
    }

    @Test
    void outsideDevModeOnlyOnRequest() {
        TestUi ui = new TestUi();
        Document doc = ui.vui.fromSource("c.vui", DOC, new Model());
        assertTrue(doc.diagnostics().isEmpty());
        Diagnostics fields = doc.checkContracts(false);
        assertEquals(3, fields.all().size(), fields.format());
    }
}
