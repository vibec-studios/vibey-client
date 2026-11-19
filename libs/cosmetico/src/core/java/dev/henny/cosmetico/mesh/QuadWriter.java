package dev.henny.cosmetico.mesh;

import dev.henny.cosmetico.model.Cube;
import org.joml.Matrix3f;
import org.joml.Vector3f;

final class QuadWriter {
    private final Matrix3f rot = new Matrix3f();
    private final Vector3f tmp = new Vector3f();
    private boolean rotated;
    private float cpx, cpy, cpz;
    private float bpx, bpy, bpz;

    void begin(Cube c, float[] bonePivotM) {
        bpx = bonePivotM[0];
        bpy = bonePivotM[1];
        bpz = bonePivotM[2];
        rotated = c.rotation != null;
        if (!rotated) return;
        float[] cp = ModelSpace.toModel(c.pivot);
        cpx = cp[0];
        cpy = cp[1];
        cpz = cp[2];
        float[] r = ModelSpace.rotToModel(c.rotation);
        rot.rotationZYX(r[2], r[1], r[0]);
    }

    void quad(FloatList out, boolean mirror, float nx, float ny, float nz,
              float ax, float ay, float az, float bx, float by, float bz,
              float cx, float cy, float cz, float dx, float dy, float dz,
              float U1, float V1, float U2, float V2, float tw, float th) {
        if (mirror) nx = -nx;
        float[] px = {ax, bx, cx, dx}, py = {ay, by, cy, dy}, pz = {az, bz, cz, dz};
        float[] us = {U2 / tw, U1 / tw, U1 / tw, U2 / tw}, vs = {V1 / th, V1 / th, V2 / th, V2 / th};
        float rnx = nx, rny = ny, rnz = nz;
        if (rotated) {
            tmp.set(nx, ny, nz);
            rot.transform(tmp);
            rnx = tmp.x;
            rny = tmp.y;
            rnz = tmp.z;
        }
        for (int k = 0; k < 4; k++) {
            int i = mirror ? 3 - k : k;
            float X = px[i], Y = py[i], Z = pz[i];
            if (rotated) {
                tmp.set(X - cpx, Y - cpy, Z - cpz);
                rot.transform(tmp);
                X = tmp.x + cpx;
                Y = tmp.y + cpy;
                Z = tmp.z + cpz;
            }
            out.add((X - bpx) / 16f, (Y - bpy) / 16f, (Z - bpz) / 16f, us[i], vs[i], rnx, rny, rnz);
        }
    }
}
