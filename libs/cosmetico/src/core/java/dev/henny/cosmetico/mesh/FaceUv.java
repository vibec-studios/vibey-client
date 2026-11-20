package dev.henny.cosmetico.mesh;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Cube;
import dev.henny.cosmetico.model.Face;
import dev.henny.cosmetico.model.Texture;

final class FaceUv {
    private FaceUv() {}

    static void write(QuadWriter q, MeshBuilder lists, Bundle bundle, Cube c, Box box) {
        float x0 = box.x0(), y0 = box.y0(), z0 = box.z0(), x1 = box.x1(), y1 = box.y1(), z1 = box.z1();
        for (int f = 0; f < 6; f++) {
            Face face = c.faces[f];
            if (face == null) continue;
            Texture t = bundle.textures[face.texture()];
            FloatList out = lists.list(face.texture());
            float a = face.u1(), bb = face.v1(), cc = face.u2(), dd = face.v2();
            float tw = t.width, th = t.height;
            switch (f) {
                case 0 -> q.quad(out, false, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, a, bb, cc, dd, tw, th);
                case 1 -> q.quad(out, false, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, a, bb, cc, dd, tw, th);
                case 2 -> q.quad(out, false, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, a, bb, cc, dd, tw, th);
                case 3 -> q.quad(out, false, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, a, bb, cc, dd, tw, th);
                case 4 -> q.quad(out, false, 0, -1, 0, x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0, cc, dd, a, bb, tw, th);
                default -> q.quad(out, false, 0, 1, 0, x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1, cc, dd, a, bb, tw, th);
            }
        }
    }
}
