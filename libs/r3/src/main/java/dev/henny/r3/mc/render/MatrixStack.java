package dev.henny.r3.mc.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

final class MatrixStack {
    private final List<Matrix4f> pool = new ArrayList<>();
    private int depth;

    MatrixStack() {
        pool.add(new Matrix4f());
    }

    void reset(Matrix4fc base) {
        depth = 0;
        pool.get(0).set(base);
    }

    Matrix4f top() {
        return pool.get(depth);
    }

    void push() {
        if (depth + 1 == pool.size()) pool.add(new Matrix4f());
        pool.get(depth + 1).set(pool.get(depth));
        depth++;
    }

    void pop() {
        if (depth == 0) throw new IllegalStateException("r3 canvas: pop without push");
        depth--;
    }

    int depth() {
        return depth;
    }

    void translate(float x, float y, float z) {
        top().translate(x, y, z);
    }

    void rotate(Quaternionfc q) {
        top().rotate(q);
    }

    void scale(float x, float y, float z) {
        top().scale(x, y, z);
    }

    void multiply(Matrix4fc m) {
        top().mul(m);
    }

    void transform(float x, float y, float z, Vector3f out) {
        top().transformPosition(x, y, z, out);
    }
}
