package dev.henny.vui.json;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourceCursor;
import dev.henny.vui.diag.SourcePos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PosJsonParser {
    private final SourceCursor in;
    private final Diagnostics diagnostics;

    private PosJsonParser(SourceCursor in, Diagnostics diagnostics) {
        this.in = in;
        this.diagnostics = diagnostics;
    }

    public static JsonValue parse(String text, String file, Diagnostics diagnostics) {
        return parse(text, new SourcePos(file, 1, 1), diagnostics);
    }

    public static JsonValue parse(String text, SourcePos start, Diagnostics diagnostics) {
        PosJsonParser p = new PosJsonParser(new SourceCursor(text, start), diagnostics);
        try {
            JsonValue v = p.value();
            p.in.skipWhitespace();
            if (!p.in.atEnd()) throw p.fail("unexpected text after the JSON value", "remove it or wrap everything in one object");
            return v;
        } catch (JsonSyntax e) {
            diagnostics.error(e.pos, e.getMessage(), e.hint);
            return null;
        }
    }

    private JsonValue value() {
        in.skipWhitespace();
        SourcePos pos = in.pos();
        char c = in.peek();
        if (c == '{') return new JsonValue(object(), pos);
        if (c == '[') return new JsonValue(array(), pos);
        if (c == '"') return new JsonValue(string(), pos);
        if (c == '-' || Character.isDigit(c)) return new JsonValue(number(), pos);
        if (in.startsWith("true")) return keyword(4, Boolean.TRUE, pos);
        if (in.startsWith("false")) return keyword(5, Boolean.FALSE, pos);
        if (in.startsWith("null")) return keyword(4, null, pos);
        if (in.atEnd()) throw fail("unexpected end of JSON", "a value, } or ] is missing");
        throw fail("unexpected character '" + c + "'", "JSON needs double quotes around strings and keys");
    }

    private JsonValue keyword(int length, Object v, SourcePos pos) {
        in.skip(length);
        return new JsonValue(v, pos);
    }

    private Map<String, JsonValue> object() {
        Map<String, JsonValue> out = new LinkedHashMap<>();
        in.next();
        in.skipWhitespace();
        if (in.peek() == '}') {
            in.next();
            return out;
        }
        while (true) {
            in.skipWhitespace();
            if (in.peek() != '"') throw fail("expected a quoted key", "write keys like \"name\"");
            String key = string();
            in.skipWhitespace();
            expect(':');
            out.put(key, value());
            in.skipWhitespace();
            char c = in.atEnd() ? '\0' : in.next();
            if (c == '}') return out;
            if (c != ',') throw fail("expected ',' or '}' in object", "a comma between entries is missing");
            in.skipWhitespace();
            if (in.peek() == '}') throw fail("trailing comma in object", "remove the last comma");
        }
    }

    private List<JsonValue> array() {
        List<JsonValue> out = new ArrayList<>();
        in.next();
        in.skipWhitespace();
        if (in.peek() == ']') {
            in.next();
            return out;
        }
        while (true) {
            out.add(value());
            in.skipWhitespace();
            char c = in.atEnd() ? '\0' : in.next();
            if (c == ']') return out;
            if (c != ',') throw fail("expected ',' or ']' in array", "a comma between items is missing");
        }
    }

    private String string() {
        in.next();
        StringBuilder sb = new StringBuilder();
        while (!in.atEnd()) {
            char c = in.next();
            if (c == '"') return sb.toString();
            if (c == '\\') sb.append(escape());
            else sb.append(c);
        }
        throw fail("unterminated string", "add the closing \"");
    }

    private char escape() {
        if (in.atEnd()) throw fail("unterminated escape", null);
        char c = in.next();
        return switch (c) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'u' -> unicode();
            default -> c;
        };
    }

    private char unicode() {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            int d = Character.digit(in.atEnd() ? 'x' : in.next(), 16);
            if (d < 0) throw fail("bad \\u escape", "use four hex digits");
            v = v * 16 + d;
        }
        return (char) v;
    }

    private Double number() {
        int start = in.index();
        while (!in.atEnd() && "+-0123456789.eE".indexOf(in.peek()) >= 0) in.next();
        String s = in.text().substring(start, in.index());
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw fail("bad number '" + s + "'", null);
        }
    }

    private void expect(char c) {
        if (in.peek() != c) throw fail("expected '" + c + "'", null);
        in.next();
    }

    private JsonSyntax fail(String message, String hint) {
        return new JsonSyntax(message, hint, in.pos());
    }

    private static final class JsonSyntax extends RuntimeException {
        final String hint;
        final SourcePos pos;

        JsonSyntax(String message, String hint, SourcePos pos) {
            super(message, null, false, false);
            this.hint = hint;
            this.pos = pos;
        }
    }
}
