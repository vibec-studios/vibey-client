package dev.henny.r3.mc.render;

final class Winding {
    private Winding() {}

    static void faceOrigin(QuadItem q, int first) {
        float ax = q.x(first), ay = q.y(first), az = q.z(first);
        float bx = q.x(first + 1) - ax, by = q.y(first + 1) - ay, bz = q.z(first + 1) - az;
        float cx = q.x(first + 2) - ax, cy = q.y(first + 2) - ay, cz = q.z(first + 2) - az;
        float nx = by * cz - bz * cy;
        float ny = bz * cx - bx * cz;
        float nz = bx * cy - by * cx;
        if (nx * -ax + ny * -ay + nz * -az < 0f) q.swap(first + 1, first + 3);
    }
}
