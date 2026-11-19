package dev.henny.r3.render;

import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;

public interface WorldCanvas {
    Vector3dc cameraPosition();

    TextMetrics textMetrics();

    void push();

    void pop();

    void translate(float x, float y, float z);

    void rotate(Quaternionfc rotation);

    void scale(float x, float y, float z);

    void multiply(Matrix4fc matrix);

    void beginQuads(RenderMode mode, TextureHandle texture);

    void vertex(float x, float y, float z, float u, float v, int argb, int light);

    void endQuads();

    void glyph(int codepoint, float x, float y, int argb, int light, boolean seeThrough);

    void text(String text, float x, float y, int argb, int light, boolean seeThrough);

    PanelCanvas beginPanel(int pixelWidth, int pixelHeight, int light, float alpha);

    void endPanel();
}
