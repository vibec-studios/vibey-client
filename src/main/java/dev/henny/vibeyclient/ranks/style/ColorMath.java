package dev.henny.vibeyclient.ranks.style;

public final class ColorMath {
    private ColorMath() {}

    public static double frac(double x) {
        return x - Math.floor(x);
    }

    public static double triangle(double x) {
        return 1 - Math.abs(2 * frac(x) - 1);
    }

    public static double roundHalfUp(double x) {
        return Math.floor(x + 0.5);
    }

    public static double clamp(double x, double lo, double hi) {
        return Math.min(hi, Math.max(lo, x));
    }

    public static int sign(boolean rtl) {
        return rtl ? -1 : 1;
    }

    public static int mix(int a, int b, double k) {
        return (channel(a, b, 16, k) << 16) | (channel(a, b, 8, k) << 8) | channel(a, b, 0, k);
    }

    private static int channel(int a, int b, int shift, double k) {
        int from = (a >> shift) & 255;
        int to = (b >> shift) & 255;
        return (int) clamp(roundHalfUp(from + (to - from) * k), 0, 255);
    }

    public static int ramp(int[] colors, double v) {
        if (colors.length == 1) return colors[0];
        double scaled = clamp(v, 0, 1) * (colors.length - 1);
        int index = (int) Math.floor(scaled);
        if (index >= colors.length - 1) return colors[colors.length - 1];
        return mix(colors[index], colors[index + 1], scaled - index);
    }

    public static int hsl(double hue, double saturation, double lightness) {
        double chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
        double sector6 = frac(hue) * 6;
        double second = chroma * (1 - Math.abs((sector6 % 2) - 1));
        double offset = lightness - chroma / 2;
        int sector = (int) Math.min(5, Math.floor(sector6));
        double[] rgb = switch (sector) {
            case 0 -> new double[] {chroma, second, 0};
            case 1 -> new double[] {second, chroma, 0};
            case 2 -> new double[] {0, chroma, second};
            case 3 -> new double[] {0, second, chroma};
            case 4 -> new double[] {second, 0, chroma};
            default -> new double[] {chroma, 0, second};
        };
        return (byteOf(rgb[0], offset) << 16) | (byteOf(rgb[1], offset) << 8) | byteOf(rgb[2], offset);
    }

    private static int byteOf(double v, double offset) {
        return (int) clamp(roundHalfUp((v + offset) * 255), 0, 255);
    }

    public static int parseHex(String hex) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') return 0xFFFFFF;
        try {
            return Integer.parseInt(hex.substring(1), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public static String hex(int rgb) {
        return String.format("#%06x", rgb & 0xFFFFFF);
    }
}
