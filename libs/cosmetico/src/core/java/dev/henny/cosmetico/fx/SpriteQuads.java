package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.mesh.BundleMesh;
import dev.henny.cosmetico.model.Sprite;
import dev.henny.cosmetico.model.Texture;
import org.joml.Vector3fc;

public final class SpriteQuads {
    private SpriteQuads() {}

    public static int quad(float[] d, int o, SpritePool p, int i, float partial, double ox, double oy, double oz, Vector3fc right, Vector3fc up) {
        Sprite s = p.bundle[i].sprites[p.sprite[i]];
        Texture t = p.bundle[i].textures[s.texture()];
        float u1 = s.u1() / t.width, v1 = s.v1() / t.height, u2 = s.u2() / t.width, v2 = s.v2() / t.height;
        float x = (float) (p.px[i] + (p.x[i] - p.px[i]) * partial - ox);
        float y = (float) (p.py[i] + (p.y[i] - p.py[i]) * partial - oy);
        float z = (float) (p.pz[i] + (p.z[i] - p.pz[i]) * partial - oz);
        float life = (p.age[i] + partial) / p.life[i];
        float h = p.size[i] * 0.5f * (life > 0.8f ? (1f - life) * 5f : 1f);
        o = corner(d, o, x, y, z, -h, -h, u1, v2, right, up);
        o = corner(d, o, x, y, z, h, -h, u2, v2, right, up);
        o = corner(d, o, x, y, z, h, h, u2, v1, right, up);
        return corner(d, o, x, y, z, -h, h, u1, v1, right, up);
    }

    private static int corner(float[] d, int o, float x, float y, float z, float a, float b, float u, float v, Vector3fc r, Vector3fc up) {
        d[o] = x + r.x() * a + up.x() * b;
        d[o + 1] = y + r.y() * a + up.y() * b;
        d[o + 2] = z + r.z() * a + up.z() * b;
        d[o + 3] = u;
        d[o + 4] = v;
        d[o + 5] = 0;
        d[o + 6] = 1;
        d[o + 7] = 0;
        return o + BundleMesh.STRIDE;
    }
}
