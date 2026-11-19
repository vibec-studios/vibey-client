package dev.henny.cosmetico.mesh;

import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Cube;

public final class BundleMesh {
    public static final int STRIDE = 8;

    public final Bundle bundle;
    public final MeshPart[][] parts;
    public final float[][] pivotM;
    public final float[][] restRotM;

    private BundleMesh(Bundle bundle, MeshPart[][] parts, float[][] pivotM, float[][] restRotM) {
        this.bundle = bundle;
        this.parts = parts;
        this.pivotM = pivotM;
        this.restRotM = restRotM;
    }

    public static BundleMesh compile(Bundle b) {
        Bone[] bones = b.model == null ? new Bone[0] : b.model.bones;
        MeshPart[][] parts = new MeshPart[bones.length][];
        float[][] piv = new float[bones.length][];
        float[][] rot = new float[bones.length][];
        for (Bone bone : bones) {
            float[] p = ModelSpace.toModel(bone.pivot);
            piv[bone.index] = p;
            rot[bone.index] = ModelSpace.rotToModel(bone.rotation);
            MeshBuilder builder = new MeshBuilder(b, mode(bone));
            for (Cube c : bone.cubes) builder.cube(c, p);
            parts[bone.index] = builder.build();
        }
        return new BundleMesh(b, parts, piv, rot);
    }

    static MeshMode mode(Bone bone) {
        if (bone.effectiveEmissive) return bone.effectiveTranslucent ? MeshMode.GLOW : MeshMode.EMISSIVE;
        return bone.effectiveTranslucent ? MeshMode.TRANSLUCENT : MeshMode.CUTOUT;
    }
}
