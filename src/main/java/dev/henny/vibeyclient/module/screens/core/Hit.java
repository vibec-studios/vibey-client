package dev.henny.vibeyclient.module.screens.core;

import java.util.List;

public final class Hit {
    public enum Zone { NONE, BODY, LEFT, RIGHT }

    public static final float EDGE = 3;

    public record Result(int index, Zone zone) {
        public static final Result NONE = new Result(-1, Zone.NONE);
    }

    private Hit() {}

    public static Result find(List<Box> boxes, List<Boolean> resizable, double px, double py) {
        int best = -1;
        float bestArea = Float.MAX_VALUE;
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            if (!grown(b).contains(px, py)) continue;
            float area = b.w() * b.h();
            if (area < bestArea) {
                bestArea = area;
                best = i;
            }
        }
        if (best < 0) return Result.NONE;
        Box b = boxes.get(best);
        if (resizable.get(best) && b.w() >= EDGE * 4) {
            if (Math.abs(px - b.x()) <= EDGE) return new Result(best, Zone.LEFT);
            if (Math.abs(px - b.right()) <= EDGE) return new Result(best, Zone.RIGHT);
        }
        return new Result(best, Zone.BODY);
    }

    private static Box grown(Box b) {
        return new Box(b.x() - 1, b.y() - 1, b.w() + 2, b.h() + 2);
    }
}
