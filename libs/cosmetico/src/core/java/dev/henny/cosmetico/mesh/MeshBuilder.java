package dev.henny.cosmetico.mesh;

import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Cube;
import dev.henny.cosmetico.model.Texture;

import java.util.ArrayList;
import java.util.List;

final class MeshBuilder {
    private final Bundle bundle;
    private final MeshMode mode;
    private final List<Integer> textures = new ArrayList<>();
    private final List<FloatList> lists = new ArrayList<>();
    private final QuadWriter writer = new QuadWriter();

    MeshBuilder(Bundle bundle, MeshMode mode) {
        this.bundle = bundle;
        this.mode = mode;
    }

    MeshPart[] build() {
        MeshPart[] out = new MeshPart[lists.size()];
        for (int i = 0; i < out.length; i++) {
            FloatList l = lists.get(i);
            out[i] = new MeshPart(textures.get(i), mode, l.toArray(), l.size() / (4 * BundleMesh.STRIDE));
        }
        return out;
    }

    FloatList list(int texture) {
        int i = textures.indexOf(texture);
        if (i >= 0) return lists.get(i);
        textures.add(texture);
        FloatList l = new FloatList();
        lists.add(l);
        return l;
    }

    void cube(Cube c, float[] bonePivotM) {
        writer.begin(c, bonePivotM);
        float x = -c.to[0], y = 24f - c.to[1], z = c.from[2];
        float w = c.to[0] - c.from[0], h = c.to[1] - c.from[1], d = c.to[2] - c.from[2];
        Box box = new Box(x - c.inflate, y - c.inflate, z - c.inflate, x + w + c.inflate, y + h + c.inflate, z + d + c.inflate);
        if (c.box != null) {
            Texture t = bundle.textures[c.texture];
            BoxUv.write(writer, list(c.texture), c.box[0], c.box[1], w, h, d, box, c.mirror, t.width, t.height);
        } else {
            FaceUv.write(writer, this, bundle, c, box);
        }
    }
}
