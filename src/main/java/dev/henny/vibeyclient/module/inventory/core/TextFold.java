package dev.henny.vibeyclient.module.inventory.core;

import java.text.Normalizer;
import java.util.Locale;

public final class TextFold {
    private TextFold() {}

    public static String fold(String s) {
        if (s == null || s.isEmpty()) return "";
        String lower = s.toLowerCase(Locale.ROOT).replace("ß", "ss");
        String plain = Normalizer.normalize(lower, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) out.append(c == '_' ? ' ' : c);
        }
        return out.toString().trim();
    }
}
