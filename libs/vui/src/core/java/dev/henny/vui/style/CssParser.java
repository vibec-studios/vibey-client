package dev.henny.vui.style;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.diag.SourceCursor;
import dev.henny.vui.diag.SourcePos;

import java.util.ArrayList;
import java.util.List;

public final class CssParser {
    private final SourceCursor in;
    private final Diagnostics diagnostics;
    private final StyleSheet sheet;
    private int order;

    private CssParser(SourceCursor in, Diagnostics diagnostics, String source) {
        this.in = in;
        this.diagnostics = diagnostics;
        this.sheet = new StyleSheet(source, diagnostics);
    }

    public static StyleSheet parse(String css, SourcePos start, Theme theme, Diagnostics diagnostics) {
        CssParser p = new CssParser(new SourceCursor(css, start), diagnostics, start.file());
        try {
            p.sheet();
        } catch (RuntimeException e) {
            diagnostics.error(p.in.pos(), "internal style parser error: " + e, "please report this file");
        }
        if (theme != null) validate(p.sheet, theme, diagnostics);
        return p.sheet;
    }

    public static List<Declaration> parseInline(String text, SourcePos pos, Diagnostics diagnostics) {
        CssParser p = new CssParser(new SourceCursor(text + "}", pos), diagnostics, pos.file());
        return p.declarations();
    }

    private static void validate(StyleSheet sheet, Theme theme, Diagnostics diagnostics) {
        ErrorSink sink = (pos, message, hint) -> diagnostics.error(pos, message, hint);
        for (Rule r : sheet.rules()) for (Declaration d : r.declarations()) d.resolve(theme, sink);
        for (Keyframes k : sheet.allKeyframes().values()) {
            for (Keyframes.Stop s : k.stops()) for (Declaration d : s.declarations()) d.resolve(theme, sink);
        }
    }

    private void sheet() {
        while (true) {
            skipTrivia();
            if (in.atEnd()) return;
            if (in.startsWith("@keyframes")) keyframes();
            else if (in.peek() == '@') unknownAtRule();
            else rule();
        }
    }

    private void rule() {
        SourcePos pos = in.pos();
        String selectorText = readUntil('{');
        if (in.atEnd()) {
            diagnostics.error(pos, "missing '{' after selector '" + selectorText.trim() + "'", null);
            return;
        }
        in.next();
        List<Declaration> declarations = declarations();
        for (String part : CssValues.splitTopLevel(selectorText, ',')) {
            try {
                sheet.add(new Rule(SelectorParser.parse(part), declarations, order++, pos));
            } catch (StyleError e) {
                diagnostics.error(pos, e.getMessage(), e.hint());
            }
        }
    }

    private void keyframes() {
        SourcePos pos = in.pos();
        in.skip("@keyframes".length());
        String name = readUntil('{').trim();
        if (in.atEnd()) {
            diagnostics.error(pos, "missing '{' after @keyframes " + name, null);
            return;
        }
        in.next();
        List<Keyframes.Stop> stops = new ArrayList<>();
        while (true) {
            skipTrivia();
            if (in.atEnd()) {
                diagnostics.error(pos, "unclosed @keyframes " + name, "add }");
                break;
            }
            if (in.peek() == '}') {
                in.next();
                break;
            }
            SourcePos stopPos = in.pos();
            String offsets = readUntil('{');
            if (in.atEnd()) break;
            in.next();
            List<Declaration> decls = declarations();
            for (String o : CssValues.splitTopLevel(offsets, ',')) {
                float offset = offset(o.trim(), stopPos);
                if (offset >= 0) stops.add(new Keyframes.Stop(offset, decls));
            }
        }
        stops.sort((a, b) -> Float.compare(a.offset(), b.offset()));
        sheet.add(new Keyframes(name, stops, pos));
    }

    private float offset(String o, SourcePos pos) {
        if (o.equals("from")) return 0f;
        if (o.equals("to")) return 1f;
        try {
            if (o.endsWith("%")) return Float.parseFloat(o.substring(0, o.length() - 1)) / 100f;
        } catch (NumberFormatException ignored) {
        }
        diagnostics.error(pos, "bad keyframe offset '" + o + "'", "use from, to or a percentage like 50%");
        return -1f;
    }

    private void unknownAtRule() {
        SourcePos pos = in.pos();
        String text = readUntil('{');
        diagnostics.error(pos, "unsupported at-rule '" + text.trim() + "'", "only @keyframes is supported");
        if (!in.atEnd()) {
            in.next();
            declarations();
        }
    }

    private List<Declaration> declarations() {
        List<Declaration> out = new ArrayList<>();
        while (true) {
            skipTrivia();
            if (in.atEnd()) {
                diagnostics.error(in.pos(), "missing '}'", "close the rule with }");
                return out;
            }
            if (in.peek() == '}') {
                in.next();
                return out;
            }
            if (in.peek() == ';') {
                in.next();
                continue;
            }
            SourcePos pos = in.pos();
            String name = readName();
            skipTrivia();
            if (in.peek() != ':') {
                diagnostics.error(pos, "expected ':' after '" + name + "'", "write declarations as name: value;");
                readValue();
                continue;
            }
            in.next();
            String value = readValue();
            if (name.isEmpty()) diagnostics.error(pos, "missing property name", null);
            else if (value.isBlank()) diagnostics.error(pos, "missing value for '" + name + "'", null);
            else out.add(new Declaration(name, value, pos));
        }
    }

    private String readName() {
        StringBuilder sb = new StringBuilder();
        while (!in.atEnd() && (Character.isLetterOrDigit(in.peek()) || in.peek() == '-' || in.peek() == '_')) sb.append(in.next());
        if (sb.length() == 0 && !in.atEnd() && in.peek() != ':') sb.append(in.next());
        return sb.toString();
    }

    private String readValue() {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        while (!in.atEnd()) {
            char c = in.peek();
            if (depth == 0 && (c == ';' || c == '}')) break;
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (in.startsWith("/*")) {
                skipComment();
                continue;
            }
            sb.append(in.next());
        }
        if (in.peek() == ';') in.next();
        return sb.toString().trim();
    }

    private String readUntil(char stop) {
        StringBuilder sb = new StringBuilder();
        while (!in.atEnd() && in.peek() != stop) {
            if (in.startsWith("/*")) skipComment();
            else sb.append(in.next());
        }
        return sb.toString();
    }

    private void skipTrivia() {
        while (!in.atEnd()) {
            if (Character.isWhitespace(in.peek())) in.next();
            else if (in.startsWith("/*")) skipComment();
            else if (in.startsWith("//")) skipLine();
            else return;
        }
    }

    private void skipComment() {
        SourcePos pos = in.pos();
        int end = in.indexOf("*/");
        if (end < 0) {
            diagnostics.error(pos, "unclosed comment", "add */");
            in.readUntil(in.text().length());
            return;
        }
        in.readUntil(end + 2);
    }

    private void skipLine() {
        while (!in.atEnd() && in.peek() != '\n') in.next();
    }
}
