package dev.henny.vui.style;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CssParserTest {
    private static StyleSheet parse(String css, Diagnostics d) {
        return CssParser.parse(css, new SourcePos("s.vui", 10, 1), VibeTheme.create(), d);
    }

    @Test
    void parsesRulesSelectorsAndKeyframes() {
        Diagnostics d = new Diagnostics();
        StyleSheet s = parse("""
                /* comment */
                .card, #main > row:hover { padding: 4 8; background: $panel; }
                @keyframes pulse { from { opacity: 1 } 50% { opacity: .5 } to { opacity: 1 } }
                """, d);
        assertTrue(d.isEmpty(), d.format());
        assertEquals(2, s.rules().size());
        assertEquals(".card", s.rules().get(0).selector().toString());
        Keyframes k = s.keyframes("pulse");
        assertNotNull(k);
        assertEquals(3, k.stops().size());
        assertEquals(0.5f, k.stops().get(1).offset());
    }

    @Test
    void reportsUnknownPropertyWithSuggestionAndLine() {
        Diagnostics d = new Diagnostics();
        parse(".a {\n  backgroud: red;\n}", d);
        assertEquals(1, d.size(), d.format());
        assertEquals(11, d.all().get(0).pos().line());
        assertTrue(d.all().get(0).hint().contains("background"), d.format());
    }

    @Test
    void reportsBadValuesAndUnknownTokens() {
        Diagnostics d = new Diagnostics();
        parse(".a { width: wide; color: $brnd; display: flexy; }", d);
        String all = d.format();
        assertEquals(3, d.size(), all);
        assertTrue(all.contains("not a length"), all);
        assertTrue(all.contains("unknown theme token $brnd"), all);
        assertTrue(all.contains("did you mean $brand"), all);
        assertTrue(all.contains("one of: column, row"), all);
    }

    @Test
    void selectorSpecificity() {
        assertEquals(1, SelectorParser.parse("row").specificity());
        assertEquals(100, SelectorParser.parse(".a").specificity());
        assertEquals(201, SelectorParser.parse("row.a:hover").specificity());
        assertEquals(10000, SelectorParser.parse("#x").specificity());
        assertEquals(101, SelectorParser.parse("column > .a").specificity());
    }

    @Test
    void siblingSelectorsParse() {
        assertEquals(200, SelectorParser.parse(".a + .b").specificity());
        assertEquals(200, SelectorParser.parse(".a~.b").specificity());
        assertTrue(SelectorParser.parse("a ~ b").usesSiblings());
        assertThrows(StyleError.class, () -> SelectorParser.parse("a + "));
    }

    @Test
    void colorsParseInAllForms() {
        assertEquals(0xFFFF0000, ColorParser.parse("#f00"));
        assertEquals(0x88FF0000, ColorParser.parse("#f008"));
        assertEquals(0xFF8B6CFF, ColorParser.parse("#8b6cff"));
        assertEquals(0x17FFFFFF, ColorParser.parse("#ffffff17"));
        assertEquals(0x80FFFFFF, ColorParser.parse("rgba(255, 255, 255, 0.5)"));
        assertEquals(0x808B6CFF, ColorParser.parse("alpha(#8b6cff, 0.5)"));
        assertEquals(0x00000000, ColorParser.parse("transparent"));
    }

    @Test
    void shorthandsExpand() {
        List<Shorthands.Longhand> p = Shorthands.expand("padding", "1 2 3");
        assertEquals("1", p.get(0).value());
        assertEquals("2", p.get(1).value());
        assertEquals("3", p.get(2).value());
        assertEquals("2", p.get(3).value());
        List<Shorthands.Longhand> b = Shorthands.expand("border", "1 #ffffff17");
        assertEquals(Prop.BORDER_COLOR, b.get(4).prop());
        List<Shorthands.Longhand> g = Shorthands.expand("background", "#000 #222 horizontal");
        assertEquals(Prop.GRADIENT_TO, g.get(1).prop());
    }

    @Test
    void transitionsAndAnimationsParse() {
        List<TransitionSpec> t = TransitionSpec.parseList("background 200ms linear 50ms, opacity");
        assertEquals(3, t.size());
        assertEquals(Prop.BACKGROUND, t.get(0).prop());
        assertEquals(Prop.GRADIENT_TO, t.get(1).prop());
        assertEquals(0.2f, t.get(0).duration(), 1e-6);
        assertEquals(0.05f, t.get(0).delay(), 1e-6);
        assertEquals(0.15f, t.get(2).duration(), 1e-6);
        List<AnimationSpec> a = AnimationSpec.parseList("pulse 1s infinite alternate out");
        assertTrue(a.get(0).infinite());
        assertTrue(a.get(0).alternate());
    }

    @Test
    void themeTokensResolveRecursively() {
        Theme t = new Theme().put("a", "$b").put("b", "#fff");
        assertEquals("#fff", t.substitute("$a"));
        Theme loop = new Theme().put("x", "$y").put("y", "$x");
        assertThrows(StyleError.class, () -> loop.substitute("$x"));
    }

    @Test
    void themeLoaderReadsJsonAndTokenFiles() {
        Diagnostics d = new Diagnostics();
        Theme fromJson = ThemeLoader.load("t.json", "{ \"colors\": { \"brand\": \"#ff0000\" }, \"pad\": 6 }", VibeTheme.create(), d);
        assertEquals("#ff0000", fromJson.get("brand"));
        assertEquals("6", fromJson.get("pad"));
        Theme fromLines = ThemeLoader.load("t.vss", "$brand: #00ff00;\n// c\n$pad: 2;", null, d);
        assertEquals("#00ff00", fromLines.get("brand"));
        assertTrue(d.isEmpty(), d.format());
    }

    @Test
    void neverThrowsOnBrokenCss() {
        String[] inputs = {"{", ".a {", ".a { color: }", "@media x { }", ".a { color: red", "/* open", ":::"};
        for (String css : inputs) parse(css, new Diagnostics());
    }
}
