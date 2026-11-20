package dev.henny.vibeyclient.ui.hud;

public enum Anchor {
    TOP_LEFT(0, 0), TOP(1, 0), TOP_RIGHT(2, 0),
    LEFT(0, 1), CENTER(1, 1), RIGHT(2, 1),
    BOTTOM_LEFT(0, 2), BOTTOM(1, 2), BOTTOM_RIGHT(2, 2);

    public final int col;
    public final int row;

    Anchor(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public float fx() {
        return col / 2f;
    }

    public float fy() {
        return row / 2f;
    }

    public int alignX() {
        return col - 1;
    }

    public static Anchor of(int col, int row) {
        for (Anchor a : values()) if (a.col == col && a.row == row) return a;
        return TOP_LEFT;
    }

    public static Anchor nearest(float cx, float cy, int w, int h) {
        int col = cx < w / 3f ? 0 : cx > w * 2f / 3f ? 2 : 1;
        int row = cy < h / 3f ? 0 : cy > h * 2f / 3f ? 2 : 1;
        return of(col, row);
    }
}
