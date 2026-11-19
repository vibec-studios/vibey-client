package dev.henny.r3.object;

import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextureHandle;

import java.util.Arrays;

public final class MeshBuilder {
    private final RenderMode mode;
    private final TextureHandle texture;
    private float[] vertices = new float[64 * Mesh.FLOATS_PER_VERTEX];
    private int[] colors = new int[64];
    private int count;

    MeshBuilder(RenderMode mode, TextureHandle texture) {
        this.mode = mode;
        this.texture = texture;
    }

    public MeshBuilder vertex(float x, float y, float z, float u, float v, int argb) {
        if (count == colors.length) {
            colors = Arrays.copyOf(colors, count * 2);
            vertices = Arrays.copyOf(vertices, count * 2 * Mesh.FLOATS_PER_VERTEX);
        }
        int o = count * Mesh.FLOATS_PER_VERTEX;
        vertices[o] = x;
        vertices[o + 1] = y;
        vertices[o + 2] = z;
        vertices[o + 3] = u;
        vertices[o + 4] = v;
        colors[count++] = argb;
        return this;
    }

    public MeshBuilder quad(float x0, float y0, float x1, float y1, float z, int argb) {
        return vertex(x0, y1, z, 0, 0, argb)
                .vertex(x0, y0, z, 0, 1, argb)
                .vertex(x1, y0, z, 1, 1, argb)
                .vertex(x1, y1, z, 1, 0, argb);
    }

    public MeshBuilder box(float halfX, float halfY, float halfZ, int argb) {
        quad(-halfX, -halfY, halfX, halfY, halfZ, argb);
        vertex(halfX, halfY, -halfZ, 0, 0, argb).vertex(halfX, -halfY, -halfZ, 0, 1, argb)
                .vertex(-halfX, -halfY, -halfZ, 1, 1, argb).vertex(-halfX, halfY, -halfZ, 1, 0, argb);
        vertex(-halfX, halfY, -halfZ, 0, 0, argb).vertex(-halfX, halfY, halfZ, 0, 1, argb)
                .vertex(halfX, halfY, halfZ, 1, 1, argb).vertex(halfX, halfY, -halfZ, 1, 0, argb);
        vertex(-halfX, -halfY, halfZ, 0, 0, argb).vertex(-halfX, -halfY, -halfZ, 0, 1, argb)
                .vertex(halfX, -halfY, -halfZ, 1, 1, argb).vertex(halfX, -halfY, halfZ, 1, 0, argb);
        vertex(halfX, halfY, halfZ, 0, 0, argb).vertex(halfX, -halfY, halfZ, 0, 1, argb)
                .vertex(halfX, -halfY, -halfZ, 1, 1, argb).vertex(halfX, halfY, -halfZ, 1, 0, argb);
        return vertex(-halfX, halfY, -halfZ, 0, 0, argb).vertex(-halfX, -halfY, -halfZ, 0, 1, argb)
                .vertex(-halfX, -halfY, halfZ, 1, 1, argb).vertex(-halfX, halfY, halfZ, 1, 0, argb);
    }

    public Mesh build() {
        return new Mesh(Arrays.copyOf(vertices, count * Mesh.FLOATS_PER_VERTEX), Arrays.copyOf(colors, count), count, mode, texture);
    }
}
