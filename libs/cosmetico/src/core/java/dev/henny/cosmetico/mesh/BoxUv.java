package dev.henny.cosmetico.mesh;

final class BoxUv {
    private BoxUv() {}

    static void write(QuadWriter q, FloatList out, float u, float v, float w, float h, float d, Box b, boolean mirror, float tw, float th) {
        Box m = mirror ? b.mirrored() : b;
        float x0 = m.x0(), y0 = m.y0(), z0 = m.z0(), x1 = m.x1(), y1 = m.y1(), z1 = m.z1();
        float u0 = u, u1 = u + d, u2 = u + d + w, u3 = u + d + w + w, u4 = u + d + w + d, u5 = u + d + w + d + w;
        float v0 = v, v1 = v + d, v2 = v + d + h;
        q.quad(out, mirror, 0, -1, 0, x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0, u1, v0, u2, v1, tw, th);
        q.quad(out, mirror, 0, 1, 0, x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1, u2, v1, u3, v0, tw, th);
        q.quad(out, mirror, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v1, u1, v2, tw, th);
        q.quad(out, mirror, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u1, v1, u2, v2, tw, th);
        q.quad(out, mirror, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u2, v1, u4, v2, tw, th);
        q.quad(out, mirror, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u4, v1, u5, v2, tw, th);
    }
}
