package dev.henny.r3.render;

import org.joml.Vector3fc;

public final class Quads {
    private Quads() {}

    public static void quad(WorldCanvas c, Vector3fc center, Vector3fc right, Vector3fc up,
                            float halfWidth, float halfHeight, int argb, int light) {
        quad(c, center, right, up, halfWidth, halfHeight, 0f, 0f, 1f, 1f, argb, light);
    }

    public static void quad(WorldCanvas c, Vector3fc center, Vector3fc right, Vector3fc up,
                            float halfWidth, float halfHeight, float u0, float v0, float u1, float v1, int argb, int light) {
        corner(c, center, right, up, -halfWidth, halfHeight, u0, v0, argb, light);
        corner(c, center, right, up, -halfWidth, -halfHeight, u0, v1, argb, light);
        corner(c, center, right, up, halfWidth, -halfHeight, u1, v1, argb, light);
        corner(c, center, right, up, halfWidth, halfHeight, u1, v0, argb, light);
    }

    public static void doubleSided(WorldCanvas c, Vector3fc center, Vector3fc right, Vector3fc up,
                                   float halfWidth, float halfHeight, int argb, int light) {
        quad(c, center, right, up, halfWidth, halfHeight, argb, light);
        corner(c, center, right, up, halfWidth, halfHeight, 1f, 0f, argb, light);
        corner(c, center, right, up, halfWidth, -halfHeight, 1f, 1f, argb, light);
        corner(c, center, right, up, -halfWidth, -halfHeight, 0f, 1f, argb, light);
        corner(c, center, right, up, -halfWidth, halfHeight, 0f, 0f, argb, light);
    }

    public static void corner(WorldCanvas c, Vector3fc center, Vector3fc right, Vector3fc up,
                              float dx, float dy, float u, float v, int argb, int light) {
        c.vertex(center.x() + right.x() * dx + up.x() * dy,
                center.y() + right.y() * dx + up.y() * dy,
                center.z() + right.z() * dx + up.z() * dy, u, v, argb, light);
    }

    public static void billboard(WorldCanvas c, float x, float y, float z, Vector3fc cameraRight, Vector3fc cameraUp,
                                 float halfSize, float roll, float u0, float v0, float u1, float v1, int argb, int light) {
        float cos = (float) Math.cos(roll);
        float sin = (float) Math.sin(roll);
        float rx = (cameraRight.x() * cos + cameraUp.x() * sin) * halfSize;
        float ry = (cameraRight.y() * cos + cameraUp.y() * sin) * halfSize;
        float rz = (cameraRight.z() * cos + cameraUp.z() * sin) * halfSize;
        float ux = (cameraUp.x() * cos - cameraRight.x() * sin) * halfSize;
        float uy = (cameraUp.y() * cos - cameraRight.y() * sin) * halfSize;
        float uz = (cameraUp.z() * cos - cameraRight.z() * sin) * halfSize;
        c.vertex(x - rx + ux, y - ry + uy, z - rz + uz, u0, v0, argb, light);
        c.vertex(x - rx - ux, y - ry - uy, z - rz - uz, u0, v1, argb, light);
        c.vertex(x + rx - ux, y + ry - uy, z + rz - uz, u1, v1, argb, light);
        c.vertex(x + rx + ux, y + ry + uy, z + rz + uz, u1, v0, argb, light);
    }

    public static void ribbon(WorldCanvas c, float ax, float ay, float az, float bx, float by, float bz,
                              float halfWidthA, float halfWidthB, int argbA, int argbB, int light) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        float sx = dy * mz - dz * my;
        float sy = dz * mx - dx * mz;
        float sz = dx * my - dy * mx;
        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1e-6f) return;
        sx /= len;
        sy /= len;
        sz /= len;
        c.vertex(ax + sx * halfWidthA, ay + sy * halfWidthA, az + sz * halfWidthA, 0f, 0f, argbA, light);
        c.vertex(ax - sx * halfWidthA, ay - sy * halfWidthA, az - sz * halfWidthA, 0f, 1f, argbA, light);
        c.vertex(bx - sx * halfWidthB, by - sy * halfWidthB, bz - sz * halfWidthB, 1f, 1f, argbB, light);
        c.vertex(bx + sx * halfWidthB, by + sy * halfWidthB, bz + sz * halfWidthB, 1f, 0f, argbB, light);
    }
}
