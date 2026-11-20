package dev.henny.vui.dom.build;

import dev.henny.vui.expr.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ClassBinding {
    private ClassBinding() {
    }

    public static List<String> names(Object value) {
        List<String> out = new ArrayList<>();
        add(value, out);
        return out;
    }

    private static void add(Object value, List<String> out) {
        if (value == null) return;
        if (value instanceof CharSequence s) {
            for (String n : s.toString().trim().split("\\s+")) if (!n.isEmpty()) out.add(n);
        } else if (value instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) if (Values.truthy(Values.unwrap(e.getValue()))) out.add(String.valueOf(e.getKey()));
        } else if (value instanceof Iterable<?> list) {
            for (Object o : list) add(o, out);
        } else {
            out.add(Values.display(value));
        }
    }
}
