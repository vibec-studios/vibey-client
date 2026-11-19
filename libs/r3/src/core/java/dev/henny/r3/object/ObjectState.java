package dev.henny.r3.object;

public final class ObjectState {
    private boolean visible;
    private boolean anchored = true;
    private double distance = Double.MAX_VALUE;
    private float alpha;
    private int light;
    private boolean layoutDirty = true;
    private int laidOutWidth = -1;
    private int laidOutHeight = -1;

    public boolean visible() {
        return visible;
    }

    public boolean anchored() {
        return anchored;
    }

    public double distance() {
        return distance;
    }

    public float alpha() {
        return alpha;
    }

    public int light() {
        return light;
    }

    public void update(boolean visible, boolean anchored, double distance, float alpha, int light) {
        this.visible = visible;
        this.anchored = anchored;
        this.distance = distance;
        this.alpha = alpha;
        this.light = light;
    }

    public void hide() {
        visible = false;
        alpha = 0f;
    }

    public void markLayoutDirty() {
        layoutDirty = true;
    }

    public boolean needsLayout(int width, int height) {
        return layoutDirty || width != laidOutWidth || height != laidOutHeight;
    }

    public void laidOut(int width, int height) {
        layoutDirty = false;
        laidOutWidth = width;
        laidOutHeight = height;
    }
}
