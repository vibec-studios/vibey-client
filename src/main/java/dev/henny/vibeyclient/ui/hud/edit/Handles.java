package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vibeyclient.ui.hud.HudLayout;

public final class Handles {
    private Handles() {}

    public static final int SIZE = 5;
    static final int[] HX = {-1, 0, 1, 1, 1, 0, -1, -1};
    static final int[] HY = {-1, -1, -1, 0, 1, 1, 1, 0};

    public static int left(HudLayout.Box b) {
        return (int) Math.floor(b.x) - 1;
    }

    public static int top(HudLayout.Box b) {
        return (int) Math.floor(b.y) - 1;
    }

    public static int width(HudLayout.Box b) {
        return Math.max(4, Math.round(b.w)) + 2;
    }

    public static int height(HudLayout.Box b) {
        return Math.max(4, Math.round(b.h)) + 2;
    }

    public static boolean visible(int i, int w, int h) {
        if (i % 2 == 0) return true;
        return (i == 1 || i == 5) ? w >= 22 : h >= 18;
    }

    public static int x(int i, int x, int w) {
        return HX[i] < 0 ? x : HX[i] > 0 ? x + w - 1 : x + w / 2;
    }

    public static int y(int i, int y, int h) {
        return HY[i] < 0 ? y : HY[i] > 0 ? y + h - 1 : y + h / 2;
    }

    public static boolean vertical(int i) {
        return i == 1 || i == 5;
    }

    public static boolean horizontal(int i) {
        return i == 3 || i == 7;
    }

    public static int at(HudLayout.Box b, double mx, double my) {
        int x = left(b), y = top(b), w = width(b), h = height(b);
        int r = 4;
        for (int i = 0; i < 8; i += 2) if (near(mx, my, x(i, x, w), y(i, y, h), r)) return i;
        for (int i = 1; i < 8; i += 2) if (visible(i, w, h) && near(mx, my, x(i, x, w), y(i, y, h), r)) return i;
        boolean inY = my > y + 2 && my < y + h - 2, inX = mx > x + 2 && mx < x + w - 2;
        if (inY && Math.abs(mx - (x + w - 1)) <= 1.5) return 3;
        if (inY && Math.abs(mx - x) <= 1.5) return 7;
        if (inX && Math.abs(my - (y + h - 1)) <= 1.5) return 5;
        if (inX && Math.abs(my - y) <= 1.5) return 1;
        return -1;
    }

    private static boolean near(double mx, double my, int hx, int hy, int r) {
        return Math.abs(mx - hx) <= r && Math.abs(my - hy) <= r;
    }
}
