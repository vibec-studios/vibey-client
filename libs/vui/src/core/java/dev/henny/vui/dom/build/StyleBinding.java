package dev.henny.vui.dom.build;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.expr.Values;
import dev.henny.vui.style.CssParser;
import dev.henny.vui.style.Declaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StyleBinding {
    private StyleBinding() {
    }

    public static List<Declaration> declarations(Object value, SourcePos pos, ErrorSink errors) {
        List<Declaration> out = new ArrayList<>();
        if (value == null) return out;
        if (value instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                Object v = Values.unwrap(e.getValue());
                if (v == null || v instanceof Boolean b && !b) continue;
                out.add(new Declaration(kebab(String.valueOf(e.getKey())), Values.display(v), pos));
            }
            return out;
        }
        if (value instanceof Iterable<?> list) {
            for (Object o : list) out.addAll(declarations(o, pos, errors));
            return out;
        }
        Diagnostics d = new Diagnostics();
        out.addAll(CssParser.parseInline(Values.display(value), pos, d));
        d.all().forEach(x -> errors.report(x.pos(), x.message(), x.hint()));
        return out;
    }

    public static String kebab(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) sb.append('-').append(Character.toLowerCase(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static String camel(String name) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-') up = true;
            else {
                sb.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return sb.toString();
    }
}
