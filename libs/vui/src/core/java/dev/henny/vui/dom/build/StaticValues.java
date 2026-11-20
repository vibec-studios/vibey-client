package dev.henny.vui.dom.build;

public final class StaticValues {
    private StaticValues() {
    }

    public static Object parse(String raw) {
        if (raw == null) return Boolean.TRUE;
        if (raw.equals("true")) return Boolean.TRUE;
        if (raw.equals("false")) return Boolean.FALSE;
        if (looksNumeric(raw)) {
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
            }
        }
        return raw;
    }

    private static boolean looksNumeric(String s) {
        if (s.isEmpty() || s.length() > 24) return false;
        char c = s.charAt(0);
        return Character.isDigit(c) || (c == '-' || c == '.') && s.length() > 1;
    }
}
