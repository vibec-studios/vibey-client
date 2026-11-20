package dev.henny.vui.expr;

import dev.henny.vui.bind.ReadableValue;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class Values {
    private Values() {
    }

    public static Object unwrap(Object v) {
        return v instanceof ReadableValue<?> r ? r.get() : v;
    }

    public static boolean truthy(Object v) {
        if (v == null || v == Scope.MISSING) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0 && !Double.isNaN(n.doubleValue());
        if (v instanceof CharSequence s) return s.length() > 0;
        return true;
    }

    public static boolean isNumber(Object v) {
        return v instanceof Number;
    }

    public static double toNumber(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof Boolean b) return b ? 1 : 0;
        if (v instanceof CharSequence s) {
            try {
                return Double.parseDouble(s.toString().trim());
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        return v == null ? 0 : Double.NaN;
    }

    public static boolean looseEquals(Object a, Object b) {
        if (a == Scope.MISSING) a = null;
        if (b == Scope.MISSING) b = null;
        if (a instanceof Number x && b instanceof Number y) return x.doubleValue() == y.doubleValue();
        if (a instanceof Enum<?> e && b instanceof CharSequence s) return e.name().equalsIgnoreCase(s.toString());
        if (b instanceof Enum<?> e && a instanceof CharSequence s) return e.name().equalsIgnoreCase(s.toString());
        if (a instanceof CharSequence x && b instanceof CharSequence y) return x.toString().contentEquals(y);
        return Objects.equals(a, b);
    }

    public static int compare(Object a, Object b) {
        if (a instanceof CharSequence x && b instanceof CharSequence y) return x.toString().compareTo(y.toString());
        return Double.compare(toNumber(a), toNumber(b));
    }

    public static Object add(Object a, Object b) {
        if (a instanceof CharSequence || b instanceof CharSequence) return display(a) + display(b);
        return toNumber(a) + toNumber(b);
    }

    public static String display(Object v) {
        if (v == null || v == Scope.MISSING) return "";
        if (v instanceof Double d) return formatNumber(d);
        if (v instanceof Float f) return formatNumber(f);
        if (v instanceof Enum<?> e) return e.name().toLowerCase();
        return String.valueOf(v);
    }

    public static String formatNumber(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) return Long.toString((long) d);
        return Double.toString(d);
    }

    public static int length(Object v) {
        if (v instanceof CharSequence s) return s.length();
        if (v instanceof Collection<?> c) return c.size();
        if (v instanceof Map<?, ?> m) return m.size();
        if (v instanceof Object[] a) return a.length;
        return 0;
    }

    public static String typeName(Object v) {
        if (v == null) return "null";
        if (v == Scope.MISSING) return "undefined";
        return v.getClass().getSimpleName();
    }
}
