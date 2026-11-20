package dev.henny.vui;

import dev.henny.vui.render.Canvas;

public class CountingCanvas implements Canvas {
    public long fills;
    public long texts;
    public long glyphs;
    public long other;
    private int depth;

    public long total() {
        return fills + texts + glyphs + other;
    }

    public int depth() {
        return depth;
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        fills++;
    }

    @Override
    public void gradient(float x, float y, float w, float h, int from, int to, boolean horizontal) {
        fills++;
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float width, int argb) {
        other++;
    }

    @Override
    public void text(CharSequence text, int start, int end, float x, float y, int argb, boolean shadow, boolean bold) {
        texts++;
    }

    @Override
    public void glyph(int codepoint, float x, float y, int argb, boolean shadow, boolean bold) {
        glyphs++;
    }

    @Override
    public void texture(String id, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {
        other++;
    }

    @Override
    public void nineSlice(String id, float x, float y, float w, float h, int slice, int textureWidth, int textureHeight, int tint) {
        other++;
    }

    @Override
    public void pushScissor(float x, float y, float w, float h) {
        depth++;
    }

    @Override
    public void popScissor() {
        depth--;
    }

    @Override
    public void push() {
        depth++;
    }

    @Override
    public void pop() {
        depth--;
    }

    @Override
    public void translate(float x, float y) {
    }

    @Override
    public void scale(float sx, float sy) {
    }

    @Override
    public void rotate(float radians) {
    }

    @Override
    public void pushAlpha(float alpha) {
        depth++;
    }

    @Override
    public void popAlpha() {
        depth--;
    }
}
