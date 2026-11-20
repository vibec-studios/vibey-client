package dev.henny.vibeyclient.module.screens.core;

public final class ScreenNames {
    private ScreenNames() {}

    public static String readable(String className) {
        if (className == null || className.isBlank()) return "?";
        String n = className.substring(className.lastIndexOf('.') + 1);
        n = n.substring(n.lastIndexOf('$') + 1);
        if (n.endsWith("Screen") && n.length() > "Screen".length()) n = n.substring(0, n.length() - "Screen".length());
        StringBuilder out = new StringBuilder(n.length() + 4);
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            boolean split = i > 0 && Character.isUpperCase(c)
                    && (Character.isLowerCase(n.charAt(i - 1)) || (i + 1 < n.length() && Character.isLowerCase(n.charAt(i + 1))));
            if (split) out.append(' ');
            out.append(c);
        }
        return out.toString();
    }
}
