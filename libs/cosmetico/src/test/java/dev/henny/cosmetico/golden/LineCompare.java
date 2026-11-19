package dev.henny.cosmetico.golden;

public final class LineCompare {
    private LineCompare() {}

    public static String diff(String want, String got, double eps) {
        if (want.equals(got)) return null;
        String[] a = clean(want).split(" "), b = clean(got).split(" ");
        if (a.length != b.length) return "token count";
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(b[i])) continue;
            Double x = number(a[i]), y = number(b[i]);
            if (x == null || y == null) return "token " + i;
            if (Math.abs(x - y) > eps * Math.max(1.0, Math.abs(x))) return "value " + i + " off by " + Math.abs(x - y);
        }
        return null;
    }

    private static String clean(String s) {
        return s.replace("[", "").replace("]", "");
    }

    private static Double number(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
