package dev.henny.vibeyclient.ui.vui.theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ThemeAudit {
    public static final double TEXT = 4.5;
    public static final double UI = 3.0;
    public static final double STATUS_DISTANCE = 20;

    public record Pair(String fg, String bg, double min, double ratio) {
        public boolean pass() {
            return ratio >= min - 1e-9;
        }

        @Override
        public String toString() {
            return String.format("%s/%s %.2f%s", fg, bg, ratio, pass() ? "" : " < " + min);
        }
    }

    private static final String[][] TEXT_PAIRS = {
            {"text", "bg"}, {"text", "panel"}, {"text", "surface"}, {"text", "surface2"}, {"text", "surface3"},
            {"muted", "panel"}, {"muted", "surface"}, {"muted", "surface2"}, {"muted", "surface3"},
            {"dim", "surface"}, {"dim", "surface2"}, {"brand2", "panel"}, {"brand2", "surface"},
            {"on-brand", "brand"}, {"on-brand", "brand-hover"}, {"bg", "danger"},
            {"ok", "surface"}, {"warn", "surface"}, {"danger", "surface"}};
    private static final String[][] UI_PAIRS = {{"brand", "surface"}, {"brand", "surface3"}, {"dim", "surface3"}};
    private static final String[][] STATUS = {{"ok", "danger"}, {"warn", "danger"}, {"ok", "warn"}};

    private ThemeAudit() {}

    public static List<Pair> contrast(Map<String, String> tokens) {
        List<Pair> out = new ArrayList<>();
        int bg = Contrast.parse(tokens.get("bg"));
        for (String[] p : TEXT_PAIRS) out.add(pair(tokens, p, TEXT, bg));
        for (String[] p : UI_PAIRS) out.add(pair(tokens, p, UI, bg));
        return out;
    }

    public static double statusDistance(Map<String, String> tokens, ColorVision vision) {
        double min = Double.MAX_VALUE;
        for (String[] p : STATUS) {
            min = Math.min(min, vision.distance(Contrast.parse(tokens.get(p[0])), Contrast.parse(tokens.get(p[1]))));
        }
        return min;
    }

    public static List<Pair> failures(Map<String, String> tokens) {
        List<Pair> out = new ArrayList<>();
        for (Pair p : contrast(tokens)) if (!p.pass()) out.add(p);
        return out;
    }

    private static Pair pair(Map<String, String> tokens, String[] p, double min, int page) {
        int back = Contrast.over(Contrast.parse(tokens.get(p[1])), page);
        int front = Contrast.over(Contrast.parse(tokens.get(p[0])), back);
        return new Pair(p[0], p[1], min, Contrast.ratio(front, back));
    }
}
