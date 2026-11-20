package dev.henny.vibeyclient.module.screens.core;

import java.util.ArrayList;
import java.util.List;

public final class Snap {
    public static final float RANGE = 4;
    public static final float MARGIN = 4;
    public static final float GAP = 4;

    public record Result(float x, float y, List<Guide> guides) {}

    private record Hit(float value, float guide, float from, float to, float dist) {}

    private Snap() {}

    public static Result move(Box b, List<Box> others, float screenW, float screenH) {
        List<Guide> guides = new ArrayList<>(2);
        Hit hx = axis(true, b, others, screenW, screenH);
        Hit hy = axis(false, b, others, screenW, screenH);
        float x = hx == null ? b.x() : hx.value;
        float y = hy == null ? b.y() : hy.value;
        if (hx != null && !Float.isNaN(hx.guide)) guides.add(new Guide(true, hx.guide, hx.from, hx.to));
        if (hy != null && !Float.isNaN(hy.guide)) guides.add(new Guide(false, hy.guide, hy.from, hy.to));
        return new Result(x, y, guides);
    }

    public static Result edge(float edge, boolean vertical, Box b, List<Box> others, float screenW, float screenH) {
        float size = vertical ? screenW : screenH;
        float best = edge, bestD = RANGE + 0.01f, guide = Float.NaN, from = 0, to = vertical ? screenH : screenW;
        float[] screen = {MARGIN, size - MARGIN, size / 2f};
        for (int i = 0; i < screen.length; i++) {
            float d = Math.abs(edge - screen[i]);
            if (d < bestD) {
                bestD = d;
                best = screen[i];
                guide = i == 2 ? screen[i] : Float.NaN;
            }
        }
        for (Box o : others) {
            float a = vertical ? o.x() : o.y(), l = vertical ? o.w() : o.h();
            for (float t : new float[] {a, a + l, a + l / 2f}) {
                float d = Math.abs(edge - t);
                if (d >= bestD) continue;
                bestD = d;
                best = t;
                guide = t;
                from = Math.min(vertical ? o.y() : o.x(), vertical ? b.y() : b.x()) - 4;
                to = Math.max(vertical ? o.bottom() : o.right(), vertical ? b.bottom() : b.right()) + 4;
            }
        }
        List<Guide> g = Float.isNaN(guide) ? List.of() : List.of(new Guide(vertical, guide, from, to));
        return new Result(vertical ? best : b.x(), vertical ? b.y() : best, g);
    }

    private static Hit axis(boolean xAxis, Box b, List<Box> others, float screenW, float screenH) {
        float pos = xAxis ? b.x() : b.y(), len = xAxis ? b.w() : b.h();
        float size = xAxis ? screenW : screenH, cross = xAxis ? screenH : screenW;
        Hit best = null;
        float[][] screen = {{MARGIN, Float.NaN}, {size - MARGIN - len, Float.NaN}, {size / 2f - len / 2f, size / 2f}};
        for (float[] t : screen) best = better(best, pos, t[0], t[1], 0, cross);
        for (Box o : others) {
            float a = xAxis ? o.x() : o.y(), l = xAxis ? o.w() : o.h();
            float ca = Math.min(xAxis ? o.y() : o.x(), xAxis ? b.y() : b.x()) - 4;
            float cb = Math.max(xAxis ? o.bottom() : o.right(), xAxis ? b.bottom() : b.right()) + 4;
            best = better(best, pos, a, a, ca, cb);
            best = better(best, pos, a + l - len, a + l, ca, cb);
            best = better(best, pos, a + l / 2f - len / 2f, a + l / 2f, ca, cb);
            best = better(best, pos, a + l + GAP, a + l, ca, cb);
            best = better(best, pos, a - len - GAP, a, ca, cb);
        }
        return best;
    }

    private static Hit better(Hit best, float pos, float target, float guide, float from, float to) {
        float d = Math.abs(pos - target);
        if (d > RANGE || (best != null && d >= best.dist)) return best;
        return new Hit(target, guide, from, to, d);
    }
}
