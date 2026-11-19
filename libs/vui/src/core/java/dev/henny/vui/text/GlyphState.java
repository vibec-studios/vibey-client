package dev.henny.vui.text;

public final class GlyphState {
    public int index;
    public int count;
    public int codepoint;
    public float time;
    public float x;
    public float width;
    public float totalWidth;
    public int color;
    public float dx;
    public float dy;
    public float alpha;
    public boolean visible;

    public void begin(int index, int count, int codepoint, float time, float x, float width, float totalWidth, int color) {
        this.index = index;
        this.count = count;
        this.codepoint = codepoint;
        this.time = time;
        this.x = x;
        this.width = width;
        this.totalWidth = totalWidth;
        this.color = color;
        this.dx = 0f;
        this.dy = 0f;
        this.alpha = 1f;
        this.visible = true;
    }

    public float fraction() {
        return count <= 1 ? 0f : index / (float) (count - 1);
    }
}
