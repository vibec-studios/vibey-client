package dev.henny.vui.dom.build;

import dev.henny.vui.expr.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ForItems {
    private ForItems() {
    }

    public record Item(Object value, Object key) {
    }

    public static List<Item> of(Object source) {
        List<Item> out = new ArrayList<>();
        Object s = Values.unwrap(source);
        if (s == null) return out;
        if (s instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) out.add(new Item(list.get(i), null));
        } else if (s instanceof Iterable<?> it) {
            for (Object o : it) out.add(new Item(o, null));
        } else if (s instanceof Object[] array) {
            for (Object o : array) out.add(new Item(o, null));
        } else if (s instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) out.add(new Item(Values.unwrap(e.getValue()), e.getKey()));
        } else if (s instanceof Number n) {
            int count = (int) n.doubleValue();
            for (int i = 1; i <= count; i++) out.add(new Item((double) i, null));
        }
        return out;
    }
}
