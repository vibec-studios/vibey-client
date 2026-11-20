package dev.henny.r3.vui;

import dev.henny.r3.math.Argb;
import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.render.TextureHandle;
import dev.henny.vui.render.Canvas;

import java.util.Arrays;

public final class VuiCanvas implements Canvas {
    private static final int MAX_GRADIENT_STRIPS = 16;

    private final TextureResolver textures;
    private final NineSlice nineSlice = new NineSlice();
    private float[] alphas = new float[16];
    private int alphaDepth;
    private PanelCanvas target;

    public VuiCanvas(TextureResolver textures) {
        this.textures = textures == null ? TextureResolver.NONE : textures;
    }

    public VuiCanvas target(PanelCanvas canvas) {
        target = canvas;
        alphaDepth = 0;
        return this;
    }

    public float alpha() {
        return alphaDepth == 0 ? 1f : alphas[alphaDepth - 1];
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        target.fill(x, y, w, h, argb);
    }

    @Override
    public void gradient(float x, float y, float w, float h, int from, int to, boolean horizontal) {
        if (!horizontal) {
            target.gradient(x, y, w, h, from, to);
            return;
        }
        int strips = Math.max(1, Math.min(MAX_GRADIENT_STRIPS, (int) Math.ceil(w / 4f)));
        float sw = w / strips;
        for (int i = 0; i < strips; i++) {
            int c = Argb.lerp(from, to, (i + 0.5f) / strips);
            target.fill(x + i * sw, y, sw, h, c);
        }
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float width, int argb) {
        target.line(x1, y1, x2, y2, width, argb);
    }

    @Override
    public void text(CharSequence text, int start, int end, float x, float y, int argb, boolean shadow, boolean bold) {
        if (end <= start) return;
        target.text(GlyphStrings.slice(text, start, end), x, y, argb, shadow, bold);
    }

    @Override
    public void glyph(int codepoint, float x, float y, int argb, boolean shadow, boolean bold) {
        target.text(GlyphStrings.of(codepoint), x, y, argb, shadow, bold);
    }

    @Override
    public void texture(String id, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {
        TextureHandle handle = textures.resolve(id);
        if (handle != null) target.texture(handle, x, y, w, h, u0, v0, u1, v1, tint);
    }

    @Override
    public void nineSlice(String id, float x, float y, float w, float h, int slice, int textureWidth, int textureHeight, int tint) {
        TextureHandle handle = textures.resolve(id);
        if (handle != null) nineSlice.draw(target, handle, x, y, w, h, slice, textureWidth, textureHeight, tint);
    }

    @Override
    public void pushScissor(float x, float y, float w, float h) {
        target.pushScissor(x, y, w, h);
    }

    @Override
    public void popScissor() {
        target.popScissor();
    }

    @Override
    public void push() {
        target.push();
    }

    @Override
    public void pop() {
        target.pop();
    }

    @Override
    public void translate(float x, float y) {
        target.translate(x, y);
    }

    @Override
    public void scale(float sx, float sy) {
        target.scale(sx, sy);
    }

    @Override
    public void rotate(float radians) {
        target.rotate(radians);
    }

    @Override
    public void pushAlpha(float alpha) {
        if (alphaDepth == alphas.length) alphas = Arrays.copyOf(alphas, alphas.length * 2);
        alphas[alphaDepth] = alpha() * alpha;
        alphaDepth++;
        target.alpha(alpha());
    }

    @Override
    public void popAlpha() {
        if (alphaDepth > 0) alphaDepth--;
        target.alpha(alpha());
    }
}
