package dev.henny.r3.interact;

public enum ResizeHandle {
    NONE(0, 0),
    LEFT(-1, 0),
    RIGHT(1, 0),
    TOP(0, 1),
    BOTTOM(0, -1),
    TOP_LEFT(-1, 1),
    TOP_RIGHT(1, 1),
    BOTTOM_LEFT(-1, -1),
    BOTTOM_RIGHT(1, -1);

    private final int sx;
    private final int sy;

    ResizeHandle(int sx, int sy) {
        this.sx = sx;
        this.sy = sy;
    }

    public int sx() {
        return sx;
    }

    public int sy() {
        return sy;
    }

    public boolean corner() {
        return sx != 0 && sy != 0;
    }

    public ResizeHandle opposite() {
        return of(-sx, -sy);
    }

    public static ResizeHandle of(int sx, int sy) {
        for (ResizeHandle h : values()) if (h.sx == Integer.signum(sx) && h.sy == Integer.signum(sy)) return h;
        return NONE;
    }
}
