package dev.henny.r3.render;

public interface PanelCanvas {
    int width();

    int height();

    TextMetrics metrics();

    void fill(float x, float y, float w, float h, int argb);

    void gradient(float x, float y, float w, float h, int topArgb, int bottomArgb);

    void line(float x1, float y1, float x2, float y2, float thickness, int argb);

    void text(String text, float x, float y, int argb);

    void texture(TextureHandle texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint);

    void pushScissor(float x, float y, float w, float h);

    void popScissor();

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float x, float y);

    void alpha(float alpha);
}
