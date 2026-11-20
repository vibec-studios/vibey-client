package dev.henny.r3.text;

public final class GlyphState {
    private int index;
    private int count;
    private int line;
    private int codepoint;
    private float x;
    private float y;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float scale;
    public float roll;
    public int argb;

    public GlyphState reset(int index, int count, int line, int codepoint, float x, float y, int argb) {
        this.index = index;
        this.count = count;
        this.line = line;
        this.codepoint = codepoint;
        this.x = x;
        this.y = y;
        this.argb = argb;
        offsetX = 0f;
        offsetY = 0f;
        offsetZ = 0f;
        scale = 1f;
        roll = 0f;
        return this;
    }

    public int index() {
        return index;
    }

    public int count() {
        return count;
    }

    public int line() {
        return line;
    }

    public int codepoint() {
        return codepoint;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float progress() {
        return count <= 1 ? 0f : index / (float) (count - 1);
    }

    public boolean transformed() {
        return scale != 1f || roll != 0f || offsetZ != 0f;
    }
}
