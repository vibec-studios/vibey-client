package dev.henny.companio.brain;

final class Glob {
    private Glob() {}

    static boolean matches(String pattern, String value) {
        if (value == null) return false;
        if (pattern.indexOf('*') < 0) return pattern.equals(value) || (!pattern.contains(":") && value.equals("minecraft:" + pattern));
        return match(pattern, 0, value, 0);
    }

    private static boolean match(String p, int pi, String v, int vi) {
        while (pi < p.length()) {
            char c = p.charAt(pi);
            if (c == '*') {
                for (int k = vi; k <= v.length(); k++) if (match(p, pi + 1, v, k)) return true;
                return false;
            }
            if (vi >= v.length() || v.charAt(vi) != c) return false;
            pi++;
            vi++;
        }
        return vi == v.length();
    }
}
