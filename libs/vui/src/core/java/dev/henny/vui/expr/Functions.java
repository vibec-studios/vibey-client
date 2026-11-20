package dev.henny.vui.expr;

import dev.henny.vui.bind.Action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Functions {
    private final Map<String, Action> custom = new HashMap<>();
    private Translator translator = Translator.KEYS;

    public Functions translator(Translator translator) {
        this.translator = translator == null ? Translator.KEYS : translator;
        return this;
    }

    public Translator translator() {
        return translator;
    }

    public Functions register(String name, Action action) {
        custom.put(name, action);
        return this;
    }

    public boolean has(String name) {
        return custom.containsKey(name) || builtin(name, null) != Scope.MISSING;
    }

    public Object call(String name, Object[] args) {
        Action a = custom.get(name);
        if (a != null) return a.invoke(args);
        return builtin(name, args);
    }

    private Object builtin(String name, Object[] args) {
        boolean probe = args == null;
        Object[] a = probe ? new Object[0] : args;
        return switch (name) {
            case "t", "i18n" -> probe ? true : translate(a);
            case "len" -> probe ? true : (double) Values.length(arg(a, 0));
            case "min" -> probe ? true : Math.min(num(a, 0), num(a, 1));
            case "max" -> probe ? true : Math.max(num(a, 0), num(a, 1));
            case "clamp" -> probe ? true : Math.max(num(a, 1), Math.min(num(a, 2), num(a, 0)));
            case "abs" -> probe ? true : Math.abs(num(a, 0));
            case "round" -> probe ? true : (double) Math.round(num(a, 0));
            case "floor" -> probe ? true : Math.floor(num(a, 0));
            case "ceil" -> probe ? true : Math.ceil(num(a, 0));
            case "str" -> probe ? true : Values.display(arg(a, 0));
            case "upper" -> probe ? true : Values.display(arg(a, 0)).toUpperCase(Locale.ROOT);
            case "lower" -> probe ? true : Values.display(arg(a, 0)).toLowerCase(Locale.ROOT);
            case "fixed" -> probe ? true : String.format(Locale.ROOT, "%." + (int) num(a, 1) + "f", num(a, 0));
            case "percent" -> probe ? true : Values.formatNumber(Math.round(num(a, 0) * 100)) + "%";
            default -> Scope.MISSING;
        };
    }

    private Object translate(Object[] a) {
        String key = Values.display(arg(a, 0));
        Object[] rest = a.length > 1 ? Arrays.copyOfRange(a, 1, a.length) : new Object[0];
        return translator.translate(key, rest);
    }

    private static Object arg(Object[] a, int i) {
        return i < a.length ? a[i] : null;
    }

    private static double num(Object[] a, int i) {
        return Values.toNumber(arg(a, i));
    }
}
