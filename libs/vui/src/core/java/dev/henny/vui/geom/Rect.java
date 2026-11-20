package dev.henny.vui.geom;

public record Rect(float x, float y, float width, float height) {

    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    public float centerX() {
        return x + width / 2f;
    }

    public float centerY() {
        return y + height / 2f;
    }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }

    public boolean intersects(Rect o) {
        return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
    }

    public Rect moved(float nx, float ny) {
        return new Rect(nx, ny, width, height);
    }

    public Rect clampInside(float viewportW, float viewportH) {
        float nx = Math.max(0, Math.min(viewportW - width, x));
        float ny = Math.max(0, Math.min(viewportH - height, y));
        return new Rect(nx, ny, width, height);
    }
}
