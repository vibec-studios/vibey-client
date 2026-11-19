package dev.henny.r3.object;

import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextureHandle;

public final class Mesh implements ObjectContent {
    public static final int FLOATS_PER_VERTEX = 5;

    private final float[] vertices;
    private final int[] colors;
    private final int vertexCount;
    private final RenderMode mode;
    private final TextureHandle texture;
    private final double radius;

    Mesh(float[] vertices, int[] colors, int vertexCount, RenderMode mode, TextureHandle texture) {
        if (vertexCount % 4 != 0) throw new IllegalArgumentException("meshes are made of quads");
        this.vertices = vertices;
        this.colors = colors;
        this.vertexCount = vertexCount;
        this.mode = mode;
        this.texture = texture;
        this.radius = computeRadius();
    }

    public static MeshBuilder builder(RenderMode mode, TextureHandle texture) {
        return new MeshBuilder(mode, texture);
    }

    private double computeRadius() {
        double r = 0;
        for (int i = 0; i < vertexCount; i++) {
            int o = i * FLOATS_PER_VERTEX;
            double x = vertices[o], y = vertices[o + 1], z = vertices[o + 2];
            r = Math.max(r, Math.sqrt(x * x + y * y + z * z));
        }
        return r;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public float x(int vertex) {
        return vertices[vertex * FLOATS_PER_VERTEX];
    }

    public float y(int vertex) {
        return vertices[vertex * FLOATS_PER_VERTEX + 1];
    }

    public float z(int vertex) {
        return vertices[vertex * FLOATS_PER_VERTEX + 2];
    }

    public float u(int vertex) {
        return vertices[vertex * FLOATS_PER_VERTEX + 3];
    }

    public float v(int vertex) {
        return vertices[vertex * FLOATS_PER_VERTEX + 4];
    }

    public int color(int vertex) {
        return colors[vertex];
    }

    public RenderMode mode() {
        return mode;
    }

    public TextureHandle texture() {
        return texture;
    }

    @Override
    public boolean translucent() {
        return mode != RenderMode.CUTOUT;
    }

    @Override
    public double boundingRadius(Transform transform) {
        return radius * transform.scale();
    }
}
