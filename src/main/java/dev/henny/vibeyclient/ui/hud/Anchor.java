package dev.henny.vibeyclient.ui.hud;

/** A point of the screen and of an element: the element's point sits on the screen's point (plus an offset). */
public enum Anchor {
    TOP_LEFT(0, 0), TOP(1, 0), TOP_RIGHT(2, 0),
    LEFT(0, 1), CENTER(1, 1), RIGHT(2, 1),
    BOTTOM_LEFT(0, 2), BOTTOM(1, 2), BOTTOM_RIGHT(2, 2);

    /** 0 = left, 1 = centre, 2 = right. */
    public final int col;
    /** 0 = top, 1 = middle, 2 = bottom. */
    public final int row;

    Anchor(int col, int row) {
        this.col = col;
        this.row = row;
    }

    /** 0, 0.5 or 1. */
    public float fx() {
        return col / 2f;
    }

    public float fy() {
        return row / 2f;
    }

    /** -1 = content aligns left, 0 = centred, 1 = right (for elements whose content is narrower than their box). */
    public int alignX() {
        return col - 1;
    }

    public static Anchor of(int col, int row) {
        for (Anchor a : values()) if (a.col == col && a.row == row) return a;
        return TOP_LEFT;
    }

    /** The anchor for a box whose centre is at (cx, cy) on a screen of w × h: the nearest third in each direction. */
    public static Anchor nearest(float cx, float cy, int w, int h) {
        int col = cx < w / 3f ? 0 : cx > w * 2f / 3f ? 2 : 1;
        int row = cy < h / 3f ? 0 : cy > h * 2f / 3f ? 2 : 1;
        return of(col, row);
    }
}
