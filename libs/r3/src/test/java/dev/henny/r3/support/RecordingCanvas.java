package dev.henny.r3.support;

import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;

public final class RecordingCanvas implements WorldCanvas, PanelCanvas {
    public final Vector3d camera = new Vector3d();
    public final TextMetrics metrics = TextMetrics.monospace(6, 9);
    public final List<String> texts = new ArrayList<>();
    public final List<RenderMode> modes = new ArrayList<>();
    public boolean recordTexts = true;
    public int vertices;
    public int batches;
    public int glyphs;
    public int panels;
    public int fills;
    public int depth;
    public int maxDepth;
    public int panelWidth;
    public int panelHeight;
    public float lastX;
    public float lastY;
    public float lastZ;
    public int lastArgb;
    private boolean inBatch;

    public void reset() {
        texts.clear();
        modes.clear();
        vertices = batches = glyphs = panels = fills = 0;
    }

    @Override
    public Vector3dc cameraPosition() {
        return camera;
    }

    @Override
    public TextMetrics textMetrics() {
        return metrics;
    }

    @Override
    public void push() {
        depth++;
        maxDepth = Math.max(maxDepth, depth);
    }

    @Override
    public void pop() {
        if (--depth < 0) throw new IllegalStateException("pop without push");
    }

    @Override
    public void translate(float x, float y, float z) {}

    @Override
    public void rotate(Quaternionfc rotation) {}

    @Override
    public void scale(float x, float y, float z) {}

    @Override
    public void multiply(Matrix4fc matrix) {}

    @Override
    public void beginQuads(RenderMode mode, TextureHandle texture) {
        if (inBatch) throw new IllegalStateException("nested batch");
        inBatch = true;
        batches++;
        if (recordTexts) modes.add(mode);
    }

    @Override
    public void vertex(float x, float y, float z, float u, float v, int argb, int light) {
        if (!inBatch) throw new IllegalStateException("vertex outside batch");
        vertices++;
        lastX = x;
        lastY = y;
        lastZ = z;
        lastArgb = argb;
    }

    @Override
    public void endQuads() {
        if (!inBatch) throw new IllegalStateException("end without begin");
        inBatch = false;
    }

    @Override
    public void glyph(int codepoint, float x, float y, int argb, int light, boolean seeThrough) {
        glyphs++;
        lastArgb = argb;
    }

    @Override
    public void text(String text, float x, float y, int argb, int light, boolean seeThrough) {
        if (recordTexts) texts.add(text);
    }

    @Override
    public PanelCanvas beginPanel(int pixelWidth, int pixelHeight, int light, float alpha) {
        panels++;
        panelWidth = pixelWidth;
        panelHeight = pixelHeight;
        return this;
    }

    @Override
    public void endPanel() {}

    @Override
    public int width() {
        return panelWidth;
    }

    @Override
    public int height() {
        return panelHeight;
    }

    @Override
    public TextMetrics metrics() {
        return metrics;
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        fills++;
    }

    @Override
    public void gradient(float x, float y, float w, float h, int topArgb, int bottomArgb) {
        fills++;
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float thickness, int argb) {}

    @Override
    public void text(String text, float x, float y, int argb) {
        if (recordTexts) texts.add(text);
    }

    @Override
    public void texture(TextureHandle texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {}

    @Override
    public void pushScissor(float x, float y, float w, float h) {}

    @Override
    public void popScissor() {}

    @Override
    public void translate(float x, float y) {}

    @Override
    public void scale(float x, float y) {}

    @Override
    public void alpha(float alpha) {}
}
