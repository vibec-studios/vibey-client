package dev.henny.companio.expr;

import java.util.List;
import java.util.Map;

public final class Functions {
    private Functions() {}

    private static final Map<String, int[]> ARITY = Map.ofEntries(
            Map.entry("chance", new int[] {1, 1}),
            Map.entry("random", new int[] {0, 0}),
            Map.entry("rand", new int[] {2, 2}),
            Map.entry("min", new int[] {2, 2}),
            Map.entry("max", new int[] {2, 2}),
            Map.entry("abs", new int[] {1, 1}),
            Map.entry("clamp", new int[] {3, 3}),
            Map.entry("cooldown", new int[] {1, 1}),
            Map.entry("since", new int[] {1, 1}),
            Map.entry("holding", new int[] {1, 8}),
            Map.entry("biome", new int[] {1, 8}),
            Map.entry("is", new int[] {2, 9}),
            Map.entry("prop", new int[] {1, 1}),
            Map.entry("hour", new int[] {2, 2}));

    public static List<String> names() {
        return ARITY.keySet().stream().sorted().toList();
    }

    static int[] arity(String name) {
        return ARITY.get(name);
    }

    static Object pure(String name, Object[] a) {
        return switch (name) {
            case "min" -> Math.min(Values.number(a[0]), Values.number(a[1]));
            case "max" -> Math.max(Values.number(a[0]), Values.number(a[1]));
            case "abs" -> Math.abs(Values.number(a[0]));
            case "clamp" -> Math.max(Values.number(a[1]), Math.min(Values.number(a[2]), Values.number(a[0])));
            case "is" -> is(a);
            default -> null;
        };
    }

    private static Object is(Object[] a) {
        for (int i = 1; i < a.length; i++) if (Values.same(a[0], a[i])) return true;
        return false;
    }
}
