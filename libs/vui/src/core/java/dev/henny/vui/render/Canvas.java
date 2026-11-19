package dev.henny.vui.render;

public interface Canvas {
    void fill(float x, float y, float w, float h, int argb);

    void gradient(float x, float y, float w, float h, int from, int to, boolean horizontal);

    void line(float x1, float y1, float x2, float y2, float width, int argb);

    void text(CharSequence text, int start, int end, float x, float y, int argb, boolean shadow, boolean bold);

    void glyph(int codepoint, float x, float y, int argb, boolean shadow, boolean bold);

    void texture(String id, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint);

    void nineSlice(String id, float x, float y, float w, float h, int slice, int textureWidth, int textureHeight, int tint);

    void pushScissor(float x, float y, float w, float h);

    void popScissor();

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float sx, float sy);

    void rotate(float radians);

    void pushAlpha(float alpha);

    void popAlpha();

    default void layer() {
    }
}
