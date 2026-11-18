package dev.henny.vibeyclient.avatar.render;

import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Bone;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Cube;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Face;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of a bundle, compiled once (any thread) from Blockbench space into Minecraft model space
 * ({@code x' = -x, y' = 24 - y, z' = z}, rotations {@code (-x, -y, z)}) with vanilla's {@code ModelPart.Cube} box UV
 * layout. Every bone gets quads relative to its own pivot, in blocks, grouped by texture and render mode.
 */
public final class BundleMesh {
    public enum Mode { CUTOUT, TRANSLUCENT, EMISSIVE }

    /** Quads of one bone with one texture and mode: 4 vertices × (x, y, z, u, v, nx, ny, nz). */
    public static final class Part {
        public final int texture;
        public final Mode mode;
        public final float[] data;
        public final int quads;

        Part(int texture, Mode mode, float[] data, int quads) {
            this.texture = texture;
            this.mode = mode;
            this.data = data;
            this.quads = quads;
        }
    }

    public static final int STRIDE = 8;

    public final Bundle bundle;
    /** Per bone: its parts (may be empty). */
    public final Part[][] parts;
    /** Per bone: pivot in model space (pixels). */
    public final float[][] pivotM;
    /** Per bone: rest rotation in model space (radians, x y z for rotationZYX). */
    public final float[][] restRotM;

    private BundleMesh(Bundle bundle, Part[][] parts, float[][] pivotM, float[][] restRotM) {
        this.bundle = bundle;
        this.parts = parts;
        this.pivotM = pivotM;
        this.restRotM = restRotM;
    }

    public static BundleMesh compile(Bundle b) {
        Bone[] bones = b.model == null ? new Bone[0] : b.model.bones;
        Part[][] parts = new Part[bones.length][];
        float[][] piv = new float[bones.length][];
        float[][] rot = new float[bones.length][];
        for (Bone bone : bones) {
            float[] p = toModel(bone.pivot);
            piv[bone.index] = p;
            rot[bone.index] = rotToModel(bone.rotation);
            Mode mode = bone.effectiveEmissive ? Mode.EMISSIVE : bone.effectiveTranslucent ? Mode.TRANSLUCENT : Mode.CUTOUT;
            Builder builder = new Builder(b, mode);
            for (Cube c : bone.cubes) builder.cube(c, p);
            parts[bone.index] = builder.build();
        }
        return new BundleMesh(b, parts, piv, rot);
    }

    /** Blockbench point → model space (pixels). */
    public static float[] toModel(float[] v) {
        return new float[]{-v[0], 24f - v[1], v[2]};
    }

    /** Blockbench euler degrees → model space radians {@code (-x, -y, z)}. */
    public static float[] rotToModel(float[] r) {
        final float d = (float) (Math.PI / 180.0);
        return new float[]{-r[0] * d, -r[1] * d, r[2] * d};
    }

    // ============================================================================================ builder

    private static final class Builder {
        final Bundle b;
        final Mode mode;
        final List<Integer> textures = new ArrayList<>();
        final List<FloatList> lists = new ArrayList<>();
        // per cube transform (cube rotation about its pivot), applied to positions and normals
        final Matrix3f rot = new Matrix3f();
        final Vector3f tmp = new Vector3f();
        boolean rotated;
        float cpx, cpy, cpz; // cube pivot relative to the bone pivot (model px)
        float bpx, bpy, bpz; // bone pivot (model px)

        Builder(Bundle b, Mode mode) {
            this.b = b;
            this.mode = mode;
        }

        Part[] build() {
            Part[] out = new Part[lists.size()];
            for (int i = 0; i < out.length; i++) {
                FloatList l = lists.get(i);
                out[i] = new Part(textures.get(i), mode, l.toArray(), l.size / (4 * STRIDE));
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
            bpx = bonePivotM[0];
            bpy = bonePivotM[1];
            bpz = bonePivotM[2];
            rotated = c.rotation != null;
            if (rotated) {
                float[] cp = toModel(c.pivot);
                cpx = cp[0];
                cpy = cp[1];
                cpz = cp[2];
                float[] r = rotToModel(c.rotation);
                rot.rotationZYX(r[2], r[1], r[0]);
            }
            // model space box (vanilla's x, y, z = min corner; w, h, d = size)
            float x = -c.to[0], y = 24f - c.to[1], z = c.from[2];
            float w = c.to[0] - c.from[0], h = c.to[1] - c.from[1], d = c.to[2] - c.from[2];
            if (c.box != null) {
                Bundle.Texture t = b.textures[c.texture];
                box(list(c.texture), c.box[0], c.box[1], x, y, z, w, h, d, c.inflate, c.mirror, t.width, t.height);
            } else {
                perFace(c, x, y, z, w, h, d);
            }
        }

        /**
         * Exactly vanilla's {@code ModelPart.Cube}: vertices, the UV regions of each face and the mirror handling.
         * Faces are written as quads (vertex 0..3 in vanilla's order).
         */
        void box(FloatList out, float u, float v, float x, float y, float z, float w, float h, float d, float g,
                 boolean mirror, float tw, float th) {
            float x0 = x - g, y0 = y - g, z0 = z - g;
            float x1 = x + w + g, y1 = y + h + g, z1 = z + d + g;
            if (mirror) {
                float t = x1;
                x1 = x0;
                x0 = t;
            }
            float u0 = u, u1 = u + d, u2 = u + d + w, u3 = u + d + w + w, u4 = u + d + w + d, u5 = u + d + w + d + w;
            float v0 = v, v1 = v + d, v2 = v + d + h;
            // vertices (vanilla names): a=(x0,y0,z0) b=(x1,y0,z0) c=(x1,y1,z0) dd=(x0,y1,z0) e=(x0,y0,z1) f=(x1,y0,z1) gg=(x1,y1,z1) hh=(x0,y1,z1)
            // DOWN (model -Y = world top): f, e, a, b with (u1, v0, u2, v1)
            quad(out, mirror, 0, -1, 0, x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0, u1, v0, u2, v1, tw, th);
            // UP (world bottom): c, dd, hh, gg with (u2, v1, u3, v0)
            quad(out, mirror, 0, 1, 0, x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1, u2, v1, u3, v0, tw, th);
            // WEST: a, e, hh, dd with (u0, v1, u1, v2)
            quad(out, mirror, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v1, u1, v2, tw, th);
            // NORTH: b, a, dd, c with (u1, v1, u2, v2)
            quad(out, mirror, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u1, v1, u2, v2, tw, th);
            // EAST: f, b, c, gg with (u2, v1, u4, v2)
            quad(out, mirror, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u2, v1, u4, v2, tw, th);
            // SOUTH: e, f, gg, hh with (u4, v1, u5, v2)
            quad(out, mirror, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u4, v1, u5, v2, tw, th);
        }

        /**
         * Per-face UV in Blockbench's convention. Side faces map exactly like the box UV regions above; Blockbench's
         * up/down rectangles are the vanilla ones with both corners swapped (box UV writes them reversed).
         * Face order in the bundle: north, east, south, west, up, down (Blockbench directions; east = +X in
         * Blockbench = model -X).
         */
        void perFace(Cube c, float x, float y, float z, float w, float h, float d) {
            float g = c.inflate;
            float x0 = x - g, y0 = y - g, z0 = z - g;
            float x1 = x + w + g, y1 = y + h + g, z1 = z + d + g;
            for (int f = 0; f < 6; f++) {
                Face face = c.faces[f];
                if (face == null) continue;
                Bundle.Texture t = b.textures[face.texture()];
                FloatList out = list(face.texture());
                float a = face.u1(), bb = face.v1(), cc = face.u2(), dd = face.v2();
                float tw = t.width, th = t.height;
                switch (f) {
                    case 0 -> // north (-Z): vanilla NORTH
                            quad(out, false, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, a, bb, cc, dd, tw, th);
                    case 1 -> // east (Blockbench +X = model -X): vanilla WEST
                            quad(out, false, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, a, bb, cc, dd, tw, th);
                    case 2 -> // south: vanilla SOUTH
                            quad(out, false, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, a, bb, cc, dd, tw, th);
                    case 3 -> // west (model +X): vanilla EAST
                            quad(out, false, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, a, bb, cc, dd, tw, th);
                    case 4 -> // up (world top = model -Y): vanilla DOWN, rect swapped
                            quad(out, false, 0, -1, 0, x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0, cc, dd, a, bb, tw, th);
                    default -> // down: vanilla UP, rect swapped
                            quad(out, false, 0, 1, 0, x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1, cc, dd, a, bb, tw, th);
                }
            }
        }

        /**
         * One vanilla polygon: vertex 0 gets (U2, V1), 1 (U1, V1), 2 (U1, V2), 3 (U2, V2); mirrored cubes reverse the
         * vertex order and flip the X normal (vanilla's mirrorFacing).
         */
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

    static final class FloatList {
        float[] a = new float[256];
        int size;

        void add(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            if (size + 8 > a.length) a = java.util.Arrays.copyOf(a, a.length * 2);
            a[size++] = x;
            a[size++] = y;
            a[size++] = z;
            a[size++] = u;
            a[size++] = v;
            a[size++] = nx;
            a[size++] = ny;
            a[size++] = nz;
        }

        float[] toArray() {
            return java.util.Arrays.copyOf(a, size);
        }
    }
}
