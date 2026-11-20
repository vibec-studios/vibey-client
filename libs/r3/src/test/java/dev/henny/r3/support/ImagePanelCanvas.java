package dev.henny.r3.support;

import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.render.ResourceTexture;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.TextureHandle;
import dev.henny.vui.headless.ImageCanvas;

import java.util.ArrayList;
import java.util.List;

public final class ImagePanelCanvas implements PanelCanvas {
    public final ImageCanvas image;
    public final List<String> texts = new ArrayList<>();
    public final List<String> textures = new ArrayList<>();
    public final List<Float> alphas = new ArrayList<>();
    private final int width;
    private final int height;
    private boolean alphaPushed;

    public ImagePanelCanvas(ImageCanvas image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public TextMetrics metrics() {
        return TextMetrics.monospace(6, 9);
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        image.fill(x, y, w, h, argb);
    }

    @Override
    public void gradient(float x, float y, float w, float h, int topArgb, int bottomArgb) {
        image.gradient(x, y, w, h, topArgb, bottomArgb, false);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float thickness, int argb) {
        image.line(x1, y1, x2, y2, thickness, argb);
    }

    @Override
    public void text(String text, float x, float y, int argb) {
        texts.add(text);
        image.text(text, 0, text.length(), x, y, argb, false, false);
    }

    @Override
    public void texture(TextureHandle texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {
        String id = texture instanceof ResourceTexture r ? r.id() : String.valueOf(texture);
        textures.add(id);
        image.texture(id, x, y, w, h, u0, v0, u1, v1, tint);
    }

    @Override
    public void pushScissor(float x, float y, float w, float h) {
        image.pushScissor(x, y, w, h);
    }

    @Override
    public void popScissor() {
        image.popScissor();
    }

    @Override
    public void push() {
        image.push();
    }

    @Override
    public void pop() {
        image.pop();
    }

    @Override
    public void translate(float x, float y) {
        image.translate(x, y);
    }

    @Override
    public void scale(float x, float y) {
        image.scale(x, y);
    }

    @Override
    public void alpha(float alpha) {
        alphas.add(alpha);
        if (alphaPushed) image.popAlpha();
        image.pushAlpha(alpha);
        alphaPushed = true;
    }
}
