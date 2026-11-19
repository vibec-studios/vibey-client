package dev.henny.vui.layout;

public enum Anchor {
    TOP_LEFT(0f, 0f), TOP(0.5f, 0f), TOP_RIGHT(1f, 0f),
    LEFT(0f, 0.5f), CENTER(0.5f, 0.5f), RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f), BOTTOM(0.5f, 1f), BOTTOM_RIGHT(1f, 1f);

    private final float fx;
    private final float fy;

    Anchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }

    public float fx() {
        return fx;
    }

    public float fy() {
        return fy;
    }

    public String cssName() {
        return name().toLowerCase().replace('_', '-');
    }

    public static Anchor fromCss(String keyword) {
        if (keyword == null || keyword.isEmpty() || keyword.equals("none")) return null;
        for (Anchor a : values()) if (a.cssName().equals(keyword)) return a;
        return null;
    }

    public static Anchor nearest(float cx, float cy, float width, float height) {
        int col = cx < width / 3f ? 0 : cx > width * 2f / 3f ? 2 : 1;
        int row = cy < height / 3f ? 0 : cy > height * 2f / 3f ? 2 : 1;
        return values()[row * 3 + col];
    }

    public float x(float containerX, float containerW, float w, float offset) {
        return containerX + (containerW - w) * fx + offset;
    }

    public float y(float containerY, float containerH, float h, float offset) {
        return containerY + (containerH - h) * fy + offset;
    }
}
