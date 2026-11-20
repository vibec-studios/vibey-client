package dev.henny.vui.markup;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;

import java.util.ArrayList;
import java.util.List;

public final class Interpolation {
    private Interpolation() {
    }

    public static List<TextPart> split(String text, SourcePos start, Diagnostics diagnostics) {
        List<TextPart> parts = new ArrayList<>();
        int line = start.line();
        int col = start.col();
        int i = 0;
        StringBuilder literal = new StringBuilder();
        SourcePos literalPos = start;
        while (i < text.length()) {
            if (text.startsWith("{{", i)) {
                int end = text.indexOf("}}", i + 2);
                SourcePos open = new SourcePos(start.file(), line, col);
                if (end < 0) {
                    diagnostics.error(open, "unclosed {{ interpolation", "add }} after the expression");
                    literal.append(text, i, text.length());
                    break;
                }
                if (literal.length() > 0) parts.add(new TextPart(false, literal.toString(), literalPos));
                literal.setLength(0);
                String expr = text.substring(i + 2, end);
                parts.add(new TextPart(true, expr.trim(), open.shift(2 + leadingSpaces(expr))));
                for (int k = i; k < end + 2; k++) {
                    if (text.charAt(k) == '\n') {
                        line++;
                        col = 1;
                    } else col++;
                }
                i = end + 2;
                literalPos = new SourcePos(start.file(), line, col);
                continue;
            }
            char c = text.charAt(i++);
            literal.append(c);
            if (c == '\n') {
                line++;
                col = 1;
            } else col++;
        }
        if (literal.length() > 0) parts.add(new TextPart(false, literal.toString(), literalPos));
        return parts;
    }

    private static int leadingSpaces(String s) {
        int n = 0;
        while (n < s.length() && Character.isWhitespace(s.charAt(n))) n++;
        return n;
    }
}
