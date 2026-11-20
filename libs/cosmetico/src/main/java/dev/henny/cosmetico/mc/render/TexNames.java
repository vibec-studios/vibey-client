package dev.henny.cosmetico.mc.render;

final class TexNames {
    private TexNames() {}

    static String sanitize(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && b.length() < 48; i++) {
            char c = Character.toLowerCase(s.charAt(i));
            b.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' ? c : '_');
        }
        return b.toString();
    }
}
