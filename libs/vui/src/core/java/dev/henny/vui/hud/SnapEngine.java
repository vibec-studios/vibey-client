package dev.henny.vui.hud;

import dev.henny.vui.geom.Rect;

import java.util.ArrayList;
import java.util.List;

public final class SnapEngine {
    public static final float THRESHOLD = 4f;

    private SnapEngine() {
    }

    public record Result(Rect rect, List<SnapGuide> guides) {
    }

    public static Result snap(Rect moving, List<Rect> others, float viewportW, float viewportH, float threshold) {
        List<float[]> xs = new ArrayList<>();
        List<float[]> ys = new ArrayList<>();
        xs.add(new float[]{0, 0, viewportH});
        xs.add(new float[]{viewportW / 2f, 0, viewportH});
        xs.add(new float[]{viewportW, 0, viewportH});
        ys.add(new float[]{0, 0, viewportW});
        ys.add(new float[]{viewportH / 2f, 0, viewportW});
        ys.add(new float[]{viewportH, 0, viewportW});
        for (Rect o : others) {
            for (float x : new float[]{o.x(), o.centerX(), o.right()}) xs.add(new float[]{x, Math.min(o.y(), moving.y()), Math.max(o.bottom(), moving.bottom())});
            for (float y : new float[]{o.y(), o.centerY(), o.bottom()}) ys.add(new float[]{y, Math.min(o.x(), moving.x()), Math.max(o.right(), moving.right())});
        }
        List<SnapGuide> guides = new ArrayList<>();
        float dx = best(new float[]{moving.x(), moving.centerX(), moving.right()}, xs, threshold);
        float dy = best(new float[]{moving.y(), moving.centerY(), moving.bottom()}, ys, threshold);
        Rect snapped = new Rect(moving.x() + (Float.isNaN(dx) ? 0 : dx), moving.y() + (Float.isNaN(dy) ? 0 : dy), moving.width(), moving.height());
        if (!Float.isNaN(dx)) addGuides(guides, true, new float[]{snapped.x(), snapped.centerX(), snapped.right()}, xs);
        if (!Float.isNaN(dy)) addGuides(guides, false, new float[]{snapped.y(), snapped.centerY(), snapped.bottom()}, ys);
        return new Result(snapped, guides);
    }

    private static float best(float[] edges, List<float[]> lines, float threshold) {
        float best = Float.NaN;
        for (float edge : edges) {
            for (float[] line : lines) {
                float d = line[0] - edge;
                if (Math.abs(d) <= threshold && (Float.isNaN(best) || Math.abs(d) < Math.abs(best))) best = d;
            }
        }
        return best;
    }

    private static void addGuides(List<SnapGuide> out, boolean vertical, float[] edges, List<float[]> lines) {
        for (float edge : edges) {
            for (float[] line : lines) {
                if (Math.abs(line[0] - edge) < 0.01f) out.add(new SnapGuide(vertical, line[0], line[1], line[2]));
            }
        }
    }
}
