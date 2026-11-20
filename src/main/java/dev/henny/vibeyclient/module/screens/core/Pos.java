package dev.henny.vibeyclient.module.screens.core;

public record Pos(int ax, int ay, float ox, float oy) {
    public static Pos of(Box b, float screenW, float screenH) {
        int ax = third(b.cx(), screenW);
        int ay = third(b.cy(), screenH);
        return new Pos(ax, ay, offset(ax, b.x(), b.w(), screenW), offset(ay, b.y(), b.h(), screenH));
    }

    public Box resolve(float w, float h, float screenW, float screenH) {
        return new Box(start(ax, ox, w, screenW), start(ay, oy, h, screenH), w, h);
    }

    static int third(float center, float size) {
        if (center < size / 3f) return 0;
        if (center > size * 2f / 3f) return 2;
        return 1;
    }

    private static float offset(int anchor, float start, float len, float size) {
        return switch (anchor) {
            case 0 -> start;
            case 2 -> start + len - size;
            default -> start + len / 2f - size / 2f;
        };
    }

    private static float start(int anchor, float off, float len, float size) {
        return switch (anchor) {
            case 0 -> off;
            case 2 -> size + off - len;
            default -> size / 2f + off - len / 2f;
        };
    }
}
