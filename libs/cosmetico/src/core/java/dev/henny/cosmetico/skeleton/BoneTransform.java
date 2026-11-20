package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.anim.Sampler;
import dev.henny.cosmetico.mesh.ModelSpace;
import dev.henny.cosmetico.model.Track;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class BoneTransform {
    private final float[] rot = new float[3], pos = new float[3], scl = new float[3];
    private final Quaternionf q = new Quaternionf();
    private float rx, ry, rz, px, py, pz, sx, sy, sz;

    void apply(Matrix4f m, Matrix3f n, Rig rig, Mix mix, int bone, float lx, float ly, float lz, boolean root) {
        start(mix, root);
        for (int k = 0; k < mix.count; k++) {
            Track[] tracks = rig.tracks(mix.anims[k]);
            Track t = tracks == null ? null : tracks[bone];
            if (t != null) blend(t, mix.weights[k], mix.times[k], mix.anims[k].loop);
        }
        float[] rest = rig.mesh.restRotM[bone];
        final float d = ModelSpace.DEG;
        m.translate((lx - px) / 16f, (ly - py) / 16f, (lz + pz) / 16f);
        float ax = rest[0] - rx * d, ay = rest[1] - ry * d, az = rest[2] + rz * d;
        if (ax != 0 || ay != 0 || az != 0) {
            q.rotationZYX(az, ay, ax);
            m.rotate(q);
            n.rotate(q);
        }
        if (sx != 1 || sy != 1 || sz != 1) {
            m.scale(sx, sy, sz);
            if (sx != 0 && sy != 0 && sz != 0) n.scale(1f / sx, 1f / sy, 1f / sz);
        }
    }

    private void start(Mix mix, boolean root) {
        rx = ry = rz = px = py = pz = 0;
        sx = sy = sz = 1;
        if (!root || !mix.hasRootOffset) return;
        px = mix.rootOffset[0];
        py = mix.rootOffset[1];
        pz = mix.rootOffset[2];
        rx = mix.rootRotation[0];
        ry = mix.rootRotation[1];
        rz = mix.rootRotation[2];
    }

    private void blend(Track t, float w, float time, boolean loop) {
        if (t.rotation() != null) {
            Sampler.sample(t.rotation(), time, loop, rot, 0);
            rx += rot[0] * w;
            ry += rot[1] * w;
            rz += rot[2] * w;
        }
        if (t.position() != null) {
            Sampler.sample(t.position(), time, loop, pos, 0);
            px += pos[0] * w;
            py += pos[1] * w;
            pz += pos[2] * w;
        }
        if (t.scale() != null) {
            Sampler.sample(t.scale(), time, loop, scl, 0);
            sx += (scl[0] - 1f) * w;
            sy += (scl[1] - 1f) * w;
            sz += (scl[2] - 1f) * w;
        }
    }
}
