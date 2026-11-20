package dev.henny.vui.component;

import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Controls {
    private Controls() {
    }

    public static Object value(Element e) {
        return e.prop("value");
    }

    public static boolean bool(Element e, String name) {
        return Values.truthy(e.prop(name));
    }

    public static float number(Element e, String name, float fallback) {
        Object v = e.prop(name);
        if (v == null) return fallback;
        double d = Values.toNumber(v);
        return Double.isNaN(d) ? fallback : (float) d;
    }

    public static String string(Element e, String name, String fallback) {
        Object v = e.prop(name);
        return v == null ? fallback : Values.display(v);
    }

    public static void input(Element e, Object next) {
        e.prop("value", next);
        e.emit("update:value", next);
        e.emit("input", next);
    }

    public static void commit(Element e, Object next) {
        input(e, next);
        e.emit("change", next);
    }

    public static List<Option> options(Element e) {
        List<Option> out = new ArrayList<>();
        Object raw = Values.unwrap(e.prop("options"));
        if (raw instanceof Iterable<?> list) {
            for (Object o : list) out.add(Option.of(Values.unwrap(o)));
        } else if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) out.add(new Option(entry.getKey(), Values.display(entry.getValue()), null));
        } else if (raw instanceof CharSequence s) {
            for (String part : s.toString().split(",")) out.add(new Option(part.trim(), part.trim(), null));
        }
        return out;
    }

    public static int selectedIndex(Element e, List<Option> options) {
        Object value = value(e);
        for (int i = 0; i < options.size(); i++) if (Values.looseEquals(options.get(i).value(), value)) return i;
        return -1;
    }

    public record Option(Object value, String label, String icon) {
        static Option of(Object o) {
            if (o instanceof Map<?, ?> m) {
                Object value = m.containsKey("value") ? m.get("value") : m.get("id");
                Object label = m.containsKey("label") ? m.get("label") : value;
                Object icon = m.get("icon");
                return new Option(value, Values.display(label), icon == null ? null : Values.display(icon));
            }
            return new Option(o, Values.display(o), null);
        }
    }
}
