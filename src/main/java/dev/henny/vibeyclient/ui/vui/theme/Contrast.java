package dev.henny.vibeyclient.ui.vui.theme;

public final class Contrast {
    private Contrast() {}

    public static int parse(String hex) {
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() == 3 || h.length() == 4) h = expand(h);
        long v = Long.parseLong(h, 16);
        if (h.length() == 6) return (int) (0xFF000000L | v);
        return (int) (((v & 0xFF) << 24) | (v >>> 8));
    }

    public static int over(int fg, int bg) {
        float a = (fg >>> 24) / 255f;
        int r = mix((fg >> 16) & 0xFF, (bg >> 16) & 0xFF, a);
        int g = mix((fg >> 8) & 0xFF, (bg >> 8) & 0xFF, a);
        int b = mix(fg & 0xFF, bg & 0xFF, a);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static double luminance(int argb) {
        return 0.2126 * linear((argb >> 16) & 0xFF) + 0.7152 * linear((argb >> 8) & 0xFF) + 0.0722 * linear(argb & 0xFF);
    }

    public static double ratio(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    static int gamma(double linear) {
        double c = Math.max(0, Math.min(1, linear));
        double s = c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
        return (int) Math.round(s * 255);
    }

    public static String hex(int argb) {
        return String.format("#%06x", argb & 0xFFFFFF);
    }

    private static int mix(int f, int b, float a) {
        return Math.round(f * a + b * (1 - a));
    }

    private static String expand(String h) {
        StringBuilder out = new StringBuilder();
        for (char c : h.toCharArray()) out.append(c).append(c);
        return out.toString();
    }
}
