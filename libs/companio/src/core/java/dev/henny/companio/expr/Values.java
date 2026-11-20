package dev.henny.companio.expr;

public final class Values {
    private Values() {}

    public static boolean truthy(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Double d) return d != 0 && !d.isNaN();
        if (v instanceof String s) return !s.isEmpty();
        return false;
    }

    public static double number(Object v) {
        if (v instanceof Double d) return d;
        if (v instanceof Boolean b) return b ? 1 : 0;
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public static String text(Object v) {
        if (v instanceof Double d) {
            double x = d;
            return x == Math.rint(x) && Math.abs(x) < 1e15 ? Long.toString((long) x) : Double.toString(x);
        }
        return String.valueOf(v);
    }

    public static boolean same(Object a, Object b) {
        if (a instanceof String || b instanceof String) return text(a).equals(text(b));
        if (a instanceof Boolean && b instanceof Boolean) return a.equals(b);
        return number(a) == number(b);
    }
}
