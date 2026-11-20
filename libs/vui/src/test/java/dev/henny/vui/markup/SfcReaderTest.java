package dev.henny.vui.markup;

import dev.henny.vui.diag.Diagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SfcReaderTest {
    private static ComponentFile read(String src) {
        return SfcReader.read("t:doc", "doc.vui", src);
    }

    @Test
    void readsAllBlocks() {
        ComponentFile f = read("""
                <import name="card" src="mymod:widgets/card"/>
                <props>{ "title": "Untitled", "count": 3 }</props>
                <template>
                  <column><text>{{ title }}</text></column>
                </template>
                <style>.a { gap: 2; }</style>
                """);
        assertFalse(f.hasErrors(), f.diagnostics().format());
        assertEquals("mymod:widgets/card", f.components().get("card"));
        assertEquals("Untitled", f.props().get("title"));
        assertEquals(3.0, f.props().get("count"));
        assertEquals(1, f.template().size());
        assertTrue(f.style().contains(".a"));
        assertEquals(6, f.stylePos().line());
    }

    @Test
    void scriptJsonDeclaresPropsAndComponents() {
        ComponentFile f = read("""
                <script lang="json">{ "props": ["a", "b"], "components": { "row2": "x:row" } }</script>
                <template><row/></template>
                """);
        assertFalse(f.hasErrors(), f.diagnostics().format());
        assertTrue(f.props().containsKey("a"));
        assertEquals("x:row", f.components().get("row2"));
    }

    @Test
    void positionsAreLineAndColumn() {
        ComponentFile f = read("<template>\n  <column>\n    <text id=\"t\">Hi</text>\n  </column>\n</template>");
        ElementNode column = (ElementNode) f.template().get(0);
        ElementNode text = (ElementNode) column.children().get(0);
        assertEquals(2, column.pos().line());
        assertEquals(3, column.pos().col());
        assertEquals(3, text.pos().line());
        assertEquals(5, text.pos().col());
        assertEquals("doc.vui", text.pos().file());
    }

    @Test
    void attributesAreClassified() {
        ComponentFile f = read("<template><row :a=\"x\" @click.stop=\"go()\" v-if=\"y\" v-model.trim=\"name\" #header=\"{ item }\" ref=\"r\" key=\"k\" plain/></template>");
        ElementNode row = (ElementNode) f.template().get(0);
        List<Attribute> a = row.attributes();
        assertEquals(AttributeKind.BIND, a.get(0).kind());
        assertEquals(AttributeKind.EVENT, a.get(1).kind());
        assertEquals(List.of("stop"), a.get(1).modifiers());
        assertEquals(AttributeKind.IF, a.get(2).kind());
        assertEquals(AttributeKind.MODEL, a.get(3).kind());
        assertTrue(a.get(3).hasModifier("trim"));
        assertEquals(AttributeKind.SLOT, a.get(4).kind());
        assertEquals("header", a.get(4).name());
        assertEquals(AttributeKind.REF, a.get(5).kind());
        assertEquals(AttributeKind.KEY, a.get(6).kind());
        assertEquals(AttributeKind.STATIC, a.get(7).kind());
        assertEquals(null, a.get(7).value());
    }

    @Test
    void interpolationSplitsText() {
        ComponentFile f = read("<template><text>Hello {{ name }}, you have {{ n + 1 }}!</text></template>");
        ElementNode text = (ElementNode) f.template().get(0);
        TextNode t = (TextNode) text.children().get(0);
        assertEquals(5, t.parts().size());
        assertTrue(t.parts().get(1).expression());
        assertEquals("name", t.parts().get(1).text());
        assertEquals("n + 1", t.parts().get(3).text());
    }

    @Test
    void lessThanInsideInterpolationIsNotATag() {
        ComponentFile f = read("<template><text>{{ a < b ? 'x' : 'y' }}</text></template>");
        assertFalse(f.hasErrors(), f.diagnostics().format());
    }

    @Test
    void mismatchedCloseTagReportsAndRecovers() {
        ComponentFile f = read("<template>\n<column>\n  <row>\n</column>\n</template>");
        assertTrue(f.hasErrors());
        Diagnostic d = f.diagnostics().errors().get(0);
        assertTrue(d.message().contains("missing </row>"), d.format());
        assertEquals(4, d.pos().line());
    }

    @Test
    void missingTemplateIsAnError() {
        ComponentFile f = read("<style>.a{}</style>");
        assertTrue(f.diagnostics().format().contains("no <template>"));
    }

    @Test
    void unknownDirectiveAndBlockHaveHints() {
        ComponentFile f = read("<foo/><template><row v-iff=\"x\"/></template>");
        String all = f.diagnostics().format();
        assertTrue(all.contains("unknown top-level block <foo>"), all);
        assertTrue(all.contains("unknown directive 'v-iff'"), all);
        assertTrue(all.contains("hint:"), all);
    }

    @Test
    void neverThrowsOnGarbage() {
        String[] inputs = {"", "<", "<template", "<template><row a=\"x></template>", "{{", "<template>{{ x</template>",
                "<props>{ bad json</props><template/>", "</x>", "<template><!-- open"};
        for (String s : inputs) {
            ComponentFile f = read(s);
            assertTrue(f.hasErrors() || f.template().isEmpty() || !f.diagnostics().isEmpty(), "input: " + s);
        }
    }

    @Test
    void badPropsJsonPointsToLine() {
        ComponentFile f = read("<template/>\n<props>\n{ \"a\": 1,\n  \"b\" 2 }\n</props>");
        Diagnostic d = f.diagnostics().errors().get(0);
        assertEquals(4, d.pos().line(), d.format());
    }

    @Test
    void whitespaceIsCondensed() {
        ComponentFile f = read("<template>\n  <text>\n    Hello\n    world\n  </text>\n</template>");
        ElementNode text = (ElementNode) f.template().get(0);
        assertEquals("Hello world", ((TextNode) text.children().get(0)).staticText());
    }
}
