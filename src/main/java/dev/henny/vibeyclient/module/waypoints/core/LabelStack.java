package dev.henny.vibeyclient.module.waypoints.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LabelStack {
    public static final int MAX_LEVEL = 4;

    public record Label(String key, double yaw, double base, double width, double line, int lines, double distance) {
        double bottom(int level) {
            return base + level * line;
        }

        double top(int level) {
            return bottom(level) + lines * line;
        }
    }

    private record Placed(Label label, int level) {}

    private LabelStack() {}

    public static Map<String, Integer> levels(List<Label> labels) {
        List<Label> sorted = new ArrayList<>(labels);
        sorted.sort(Comparator.comparingDouble(Label::distance));
        List<Placed> placed = new ArrayList<>();
        Map<String, Integer> out = new HashMap<>();
        for (Label l : sorted) {
            int level = 0;
            while (level < MAX_LEVEL && blocked(l, level, placed)) level++;
            placed.add(new Placed(l, level));
            out.put(l.key(), level);
        }
        return out;
    }

    private static boolean blocked(Label l, int level, List<Placed> placed) {
        for (Placed p : placed) if (overlaps(l, level, p.label(), p.level())) return true;
        return false;
    }

    static boolean overlaps(Label a, int levelA, Label b, int levelB) {
        double cos = Math.cos((a.base() + b.base()) / 2);
        double dx = wrap(a.yaw() - b.yaw()) * cos;
        if (Math.abs(dx) >= (a.width() + b.width()) / 2) return false;
        return a.bottom(levelA) < b.top(levelB) && b.bottom(levelB) < a.top(levelA);
    }

    private static double wrap(double angle) {
        double a = angle % (2 * Math.PI);
        if (a > Math.PI) a -= 2 * Math.PI;
        if (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}
