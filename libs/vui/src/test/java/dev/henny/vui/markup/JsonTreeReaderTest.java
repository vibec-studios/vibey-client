package dev.henny.vui.markup;

import dev.henny.vui.TestUi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTreeReaderTest {
    @Test
    void jsonTreeCompilesToSameAst() {
        ComponentFile f = JsonTreeReader.read("t:j", "j.vui.json", """
                {
                  "props": { "title": "T" },
                  "style": [".x { gap: 2; }"],
                  "template": { "tag": "column", "attrs": { "class": "x", ":id": "'c'" },
                                "children": ["Hi {{ title }}", { "tag": "button", "attrs": { "@click": "go()" } }] }
                }
                """);
        assertFalse(f.hasErrors(), f.diagnostics().format());
        ElementNode column = (ElementNode) f.template().get(0);
        assertEquals("column", column.tag());
        assertEquals(2, column.children().size());
        assertEquals(AttributeKind.BIND, column.attributes().get(1).kind());
        assertEquals(4, column.pos().line());
    }

    @Test
    void missingTagIsReportedWithPosition() {
        ComponentFile f = JsonTreeReader.read("t:j", "j.vui.json", "{\n \"template\": { \"attrs\": {} }\n}");
        assertTrue(f.hasErrors());
        assertEquals(2, f.diagnostics().errors().get(0).pos().line());
    }

    @Test
    void jsonDocumentRenders() {
        TestUi ui = new TestUi();
        ui.doc = ui.vui.create(ui.vui.compile("t:j", "j.vui.json",
                "{ \"template\": { \"tag\": \"text\", \"attrs\": { \"id\": \"t\" }, \"children\": [\"{{ 1 + 2 }}\"] } }"), null);
        ui.doc.resize(100, 50);
        ui.doc.frame(0);
        assertEquals("3", ui.find("t").text().chars().toString());
    }
}
