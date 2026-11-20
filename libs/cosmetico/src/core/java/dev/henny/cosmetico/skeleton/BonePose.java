package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Locator;
import dev.henny.cosmetico.model.PlayerBone;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class BonePose {
    private Matrix4f[] mats = new Matrix4f[0];
    private Matrix3f[] norms = new Matrix3f[0];
    private boolean[] visible = new boolean[0], drawn = new boolean[0];
    private final BoneTransform transform = new BoneTransform();
    private final Matrix4f partPose = new Matrix4f();
    private final Matrix3f partNormal = new Matrix3f();
    private int count;

    public void compute(Rig rig, Mix mix, Matrix4fc base, Matrix3fc baseNormal, PartFrames parts, Pass pass, LocatorSink locators) {
        count = 0;
        if (rig.bundle.model == null) return;
        Bone[] bones = rig.bundle.model.bones;
        count = bones.length;
        ensure(count);
        for (int i = 0; i < count; i++) {
            if (!bone(rig, mix, bones[i], i, base, baseNormal, parts, pass)) {
                drawn[i] = visible[i] = false;
                continue;
            }
            drawn[i] = true;
            boolean vis = visible[i];
            if (vis && mix.visibility != null && mix.visibility.hasVisibility) vis = Visibility.at(mix.visibility, i, mix.visibilityTime);
            if (vis && mix.hidden != null && i < mix.hidden.length && mix.hidden[i]) vis = false;
            visible[i] = vis;
            if (locators != null) boneLocators(rig, i, locators);
        }
        if (pass == Pass.ATTACHED && parts != null && locators != null) partLocators(rig, base, baseNormal, parts, locators);
    }

    private boolean bone(Rig rig, Mix mix, Bone bone, int i, Matrix4fc base, Matrix3fc baseNormal, PartFrames parts, Pass pass) {
        if (bone.parent >= 0) {
            int p = bone.parent;
            if (!drawn[p]) return false;
            mats[i].set(mats[p]);
            norms[i].set(norms[p]);
            float[] pm = rig.mesh.pivotM[i], pp = rig.mesh.pivotM[p];
            visible[i] = visible[p];
            transform.apply(mats[i], norms[i], rig, mix, i, pm[0] - pp[0], pm[1] - pp[1], pm[2] - pp[2], false);
            return true;
        }
        if ((pass == Pass.WORLD) != (bone.attach == dev.henny.cosmetico.model.Attach.WORLD)) return false;
        PlayerBone part = bone.attach.part();
        float[] pm = rig.mesh.pivotM[i];
        float lx = pm[0], ly = pm[1], lz = pm[2];
        mats[i].set(base);
        norms[i].set(baseNormal);
        if (part != null) {
            if (parts == null) return false;
            parts.apply(part, mats[i], norms[i]);
            lx -= -part.px;
            ly -= 24f - part.py;
            lz -= part.pz;
        }
        visible[i] = true;
        transform.apply(mats[i], norms[i], rig, mix, i, lx, ly, lz, true);
        return true;
    }

    private void boneLocators(Rig rig, int i, LocatorSink locators) {
        if (rig.bundle.model.locators.isEmpty()) return;
        for (Locator l : rig.bundle.model.locators.values()) if (l.bone() == i) locators.locator(l, mats[i], rig.mesh.pivotM[i]);
    }

    private void partLocators(Rig rig, Matrix4fc base, Matrix3fc baseNormal, PartFrames parts, LocatorSink locators) {
        for (Locator l : rig.bundle.model.locators.values()) {
            if (l.bone() >= 0 || l.playerBone() == null) continue;
            partPose.set(base);
            partNormal.set(baseNormal);
            parts.apply(l.playerBone(), partPose, partNormal);
            locators.locator(l, partPose, LocatorMath.partPivot(l.playerBone()));
        }
    }

    private void ensure(int n) {
        if (mats.length >= n) return;
        int m = Math.max(n, mats.length * 2);
        Matrix4f[] nm = new Matrix4f[m];
        Matrix3f[] nn = new Matrix3f[m];
        for (int i = 0; i < m; i++) {
            nm[i] = i < mats.length ? mats[i] : new Matrix4f();
            nn[i] = i < norms.length ? norms[i] : new Matrix3f();
        }
        mats = nm;
        norms = nn;
        visible = new boolean[m];
        drawn = new boolean[m];
    }

    public int count() {
        return count;
    }

    public boolean drawn(int bone) {
        return drawn[bone];
    }

    public boolean visible(int bone) {
        return visible[bone];
    }

    public Matrix4f matrix(int bone) {
        return mats[bone];
    }

    public Matrix3f normal(int bone) {
        return norms[bone];
    }
}
