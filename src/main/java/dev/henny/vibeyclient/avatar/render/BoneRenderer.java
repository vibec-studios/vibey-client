package dev.henny.vibeyclient.avatar.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.anim.Sampler;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Animation;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Attach;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Bone;
import dev.henny.vibeyclient.avatar.bundle.Bundle.PlayerBone;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Track;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else
/*import net.minecraft.client.renderer.RenderType;*/

/**
 * Draws the bones of one bundle: computes every bone matrix (parent chain, attach point, blended animation channels),
 * applies visibility and hands the quads to a {@link MeshSink}. One instance, render thread only, scratch arrays grow
 * to the biggest bundle seen (no per-frame allocation).
 */
public final class BoneRenderer {
    private BoneRenderer() {}

    /** Up to 4 animations blended (cosmetics: idle/walk/fly/sneak; emote props: main). */
    public static final class Mix {
        public final Animation[] anims = new Animation[4];
        public final float[] times = new float[4];
        public final float[] weights = new float[4];
        public int count;
        /** Animation whose visibility events apply (usually idle / the emote), may be null. */
        public Animation visibility;
        public float visibilityTime;

        public Mix clear() {
            count = 0;
            visibility = null;
            return this;
        }

        public Mix add(Animation a, float t, float w) {
            if (a == null || w <= 0.001f || count == 4) return this;
            anims[count] = a;
            times[count] = t;
            weights[count] = w;
            count++;
            return this;
        }
    }

    /** What to draw: bones attached to the player ({@link Pass#ATTACHED}) or to the world frame ({@link Pass#WORLD}). */
    public enum Pass { ATTACHED, WORLD }

    /** Optional callback with each bone's final matrix (locator positions). */
    public interface LocatorSink {
        void locator(Bundle.Locator locator, Matrix4f boneMatrix, float[] bonePivotM);
    }

    private static Matrix4f[] mats = new Matrix4f[0];
    private static Matrix3f[] norms = new Matrix3f[0];
    private static boolean[] visible = new boolean[0];
    private static boolean[] drawn = new boolean[0];
    private static final float[] ROT = new float[3], POS = new float[3], SCL = new float[3];
    private static final Quaternionf Q = new Quaternionf();
    private static final Vector3f V = new Vector3f();

    private static void ensure(int n) {
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

    /**
     * Renders {@code gb}. The pose stack must be in the player's model space (the space of the vanilla PlayerModel
     * parts) for {@link Pass#ATTACHED}, or in a world frame already converted to model space for {@link Pass#WORLD}.
     *
     * @param parts   player parts for attach points (ATTACHED pass), may be null in the WORLD pass
     * @param color   ARGB multiplier
     */
    public static void render(GpuBundle gb, Mix mix, PoseStack ps, MeshSink sink, PlayerParts parts, Pass pass,
                              int light, int color, LocatorSink locators) {
        Bundle b = gb.bundle;
        if (b.model == null) return;
        Bone[] bones = b.model.bones;
        BundleMesh mesh = gb.mesh;
        int n = bones.length;
        ensure(n);
        for (int i = 0; i < n; i++) {
            Bone bone = bones[i];
            boolean vis;
            if (bone.parent < 0) {
                boolean world = bone.attach == Attach.WORLD;
                if ((pass == Pass.WORLD) != world) {
                    drawn[i] = false;
                    visible[i] = false;
                    continue;
                }
                PlayerBone part = bone.attach.part();
                float[] pm = mesh.pivotM[i];
                float lx = pm[0], ly = pm[1], lz = pm[2];
                if (part != null) {
                    if (parts == null) {
                        drawn[i] = visible[i] = false;
                        continue;
                    }
                    ps.pushPose();
                    parts.apply(part, ps);
                    mats[i].set(ps.last().pose());
                    norms[i].set(ps.last().normal());
                    ps.popPose();
                    // rest pivot of the part in model space
                    lx -= -part.px;
                    ly -= 24f - part.py;
                    lz -= part.pz;
                } else {
                    mats[i].set(ps.last().pose());
                    norms[i].set(ps.last().normal());
                }
                vis = true;
                transform(i, gb, mix, lx, ly, lz);
            } else {
                int p = bone.parent;
                if (!drawn[p]) {
                    drawn[i] = visible[i] = false;
                    continue;
                }
                mats[i].set(mats[p]);
                norms[i].set(norms[p]);
                float[] pm = mesh.pivotM[i], pp = mesh.pivotM[p];
                vis = visible[p];
                transform(i, gb, mix, pm[0] - pp[0], pm[1] - pp[1], pm[2] - pp[2]);
            }
            drawn[i] = true;
            if (vis && mix.visibility != null && mix.visibility.hasVisibility) vis = visibleAt(mix.visibility, i, mix.visibilityTime);
            visible[i] = vis;
            if (locators != null && !b.model.locators.isEmpty()) {
                for (Bundle.Locator l : b.model.locators.values()) if (l.bone() == i) locators.locator(l, mats[i], mesh.pivotM[i]);
            }
            if (!vis) continue;
            BundleMesh.Part[] list = mesh.parts[i];
            if (list.length == 0) continue;
            ps.pushPose();
            ps.last().pose().set(mats[i]);
            ps.last().normal().set(norms[i]);
            for (BundleMesh.Part part : list) {
                Tex tex = gb.texture(part.texture);
                if (tex == null) continue;
                RenderType type;
                int l = light;
                switch (part.mode) {
                    case EMISSIVE -> {
                        type = tex.emissive();
                        l = MeshSink.FULL_BRIGHT;
                    }
                    case TRANSLUCENT -> type = tex.translucent();
                    default -> type = tex.cutout();
                }
                sink.draw(ps, type, part, l, MeshSink.NO_OVERLAY, color);
            }
            ps.popPose();
        }
    }

    /** mats[i] *= T((local + pos) / 16) · R(rest + rot) · S(scale), blending the mix. */
    private static void transform(int i, GpuBundle gb, Mix mix, float lx, float ly, float lz) {
        float[] rest = gb.mesh.restRotM[i];
        float rx = 0, ry = 0, rz = 0, px = 0, py = 0, pz = 0, sx = 1, sy = 1, sz = 1;
        for (int k = 0; k < mix.count; k++) {
            Track[] tracks = gb.tracks(mix.anims[k]);
            Track t = tracks == null ? null : tracks[i];
            if (t == null) continue;
            float w = mix.weights[k], time = mix.times[k];
            if (t.rotation() != null) {
                Sampler.sample(t.rotation(), time, ROT, 0);
                rx += ROT[0] * w;
                ry += ROT[1] * w;
                rz += ROT[2] * w;
            }
            if (t.position() != null) {
                Sampler.sample(t.position(), time, POS, 0);
                px += POS[0] * w;
                py += POS[1] * w;
                pz += POS[2] * w;
            }
            if (t.scale() != null) {
                Sampler.sample(t.scale(), time, SCL, 0);
                sx += (SCL[0] - 1f) * w;
                sy += (SCL[1] - 1f) * w;
                sz += (SCL[2] - 1f) * w;
            }
        }
        final float d = (float) (Math.PI / 180.0);
        // Blockbench → model space: positions and rotations (-x, -y, z)
        float tx = (lx - px) / 16f, ty = (ly - py) / 16f, tz = (lz + pz) / 16f;
        Matrix4f m = mats[i];
        m.translate(tx, ty, tz);
        float ax = rest[0] - rx * d, ay = rest[1] - ry * d, az = rest[2] + rz * d;
        if (ax != 0 || ay != 0 || az != 0) {
            Q.rotationZYX(az, ay, ax);
            m.rotate(Q);
            norms[i].rotate(Q);
        }
        if (sx != 1 || sy != 1 || sz != 1) {
            m.scale(sx, sy, sz);
            // like PoseStack.scale: normals get the inverse scale (sign kept), renormalised at emit
            if (sx != 0 && sy != 0 && sz != 0) norms[i].scale(1f / sx, 1f / sy, 1f / sz);
        }
    }

    /** Visibility of bone {@code i} at {@code t}: last event at or before t (default visible). */
    private static boolean visibleAt(Animation a, int bone, float t) {
        boolean v = true;
        for (Bundle.Event e : a.events) {
            if (e.t > t + 1e-4f) break;
            if (e.type == Bundle.EventType.VISIBILITY && e.bone == bone) v = e.visible;
        }
        return v;
    }

    /** Model-space point of a locator relative to its bone's pivot, in blocks, into {@code out}. */
    public static Vector3f locatorLocal(Bundle.Locator l, float[] bonePivotM, Vector3f out) {
        float[] p = l.position();
        return out.set((-p[0] - bonePivotM[0]) / 16f, (24f - p[1] - bonePivotM[1]) / 16f, (p[2] - bonePivotM[2]) / 16f);
    }

    static Vector3f scratch() {
        return V;
    }
}
