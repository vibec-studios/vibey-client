package dev.henny.cosmetico.mesh;

import java.util.Arrays;

final class FloatList {
    private float[] a = new float[256];
    private int size;

    void add(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        if (size + 8 > a.length) a = Arrays.copyOf(a, a.length * 2);
        a[size++] = x;
        a[size++] = y;
        a[size++] = z;
        a[size++] = u;
        a[size++] = v;
        a[size++] = nx;
        a[size++] = ny;
        a[size++] = nz;
    }

    int size() {
        return size;
    }

    float[] toArray() {
        return Arrays.copyOf(a, size);
    }
}
