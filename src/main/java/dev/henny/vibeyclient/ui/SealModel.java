package dev.henny.vibeyclient.ui;

/**
 * The Vibe seal (the blocky grey seal from the launcher's {@code seal3d.ts}) as a tiny software renderer: textured
 * boxes, orthographic projection in the launcher's logo pose, rasterised into an ARGB pixel buffer. Pure Java (no
 * Minecraft classes), so it can be rendered at the exact screen-pixel density and stays crisp at every GUI scale.
 * {@link Seal} turns the buffer into a texture.
 *
 * <p>Model units are texels (the launcher's "1 unit = 1 pixel"): head points +X, +Y up, underside of the seal at
 * y = 0, ice block below it. Parts that move: head (nod/turn), front fins (clap), tail fins (wag), the whole seal
 * (hop), eyes (blink).
 */
public final class SealModel {
    // Colours from seal3d.ts (SEAL_COLORS)
    private static final int BODY = 0x8f9aa6, BACK = 0x7d8894, BELLY = 0xcdd4db, SPOT = 0x68737f, SNOUT = 0xe8ecf0,
            FLIPPER = 0x707b87, EYE = 0x141418, SHINE = 0xffffff, NOSE = 0x2a2a30, WHISKER = 0xa3acb5,
            CHEEK = 0xff9db8, ICE = 0xe3f0f9, ICE_LIGHT = 0xf6fbff, ICE_SIDE = 0xbcd9ee, ICE_DARK = 0x9cc4e2;

    /** Logo pose of the launcher (Logo3D.tsx). */
    public static final float YAW = -1.05f;
    public static final float PITCH = 0.42f;

    /** Pose of the animated parts. */
    public static final class Pose {
        public float headZ, headY, finFlap, tailWag, hop, yaw = YAW;
        public boolean blink;

        public void set(Pose o) {
            headZ = o.headZ;
            headY = o.headY;
            finFlap = o.finFlap;
            tailWag = o.tailWag;
            hop = o.hop;
            yaw = o.yaw;
            blink = o.blink;
        }

        public boolean same(Pose o) {
            return headZ == o.headZ && headY == o.headY && finFlap == o.finFlap && tailWag == o.tailWag && hop == o.hop
                    && yaw == o.yaw && blink == o.blink;
        }
    }

    // ============================================================================================ textures

    /** One face texture: w × h RGB texels. */
    private record Tex(int w, int h, int[] rgb) {}

    private static int seedState;

    private static float rnd() {
        seedState = seedState * 1664525 + 1013904223;
        return (seedState >>> 0 & 0xFFFFFFFFL) / 4294967296f;
    }

    private static Tex noisy(int w, int h, int base, float amount, int seed) {
        seedState = seed;
        int[] px = new int[w * h];
        int r = base >> 16 & 255, g = base >> 8 & 255, b = base & 255;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float f = 1 + (rnd() - 0.5f) * 2 * amount;
                px[y * w + x] = Math.min(255, (int) (r * f)) << 16 | Math.min(255, (int) (g * f)) << 8 | Math.min(255, (int) (b * f));
            }
        }
        return new Tex(w, h, px);
    }

    private static Tex px(Tex t, int color, int... xy) {
        for (int i = 0; i + 1 < xy.length; i += 2) {
            if (xy[i] < t.w && xy[i + 1] < t.h) t.rgb[xy[i + 1] * t.w + xy[i]] = color;
        }
        return t;
    }

    private static Tex rows(Tex t, int color, int y, int h) {
        for (int yy = y; yy < Math.min(t.h, y + h); yy++) for (int x = 0; x < t.w; x++) t.rgb[yy * t.w + x] = color;
        return t;
    }

    // ============================================================================================ boxes

    /** Faces in three.js BoxGeometry order: +x, -x, +y, -y, +z, -z. */
    private static final int PX = 0, NX = 1, PY = 2, NY = 3, PZ = 4, NZ = 5;

    /** Box of size w × h × d centred at (cx, cy, cz) in its part's space, with one texture per face. */
    private static final class Box {
        final float cx, cy, cz, w, h, d;
        final Tex[] faces = new Tex[6];

        Box(float cx, float cy, float cz, float w, float h, float d) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.w = w;
            this.h = h;
            this.d = d;
        }
    }

    /** A movable part: boxes in local space, placed by position + rotation (three.js Euler XYZ). */
    private static final class Part {
        final java.util.List<Box> boxes = new java.util.ArrayList<>();
        float x, y, z, rx, ry, rz;
        Part parent;

        Part(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private final Part seal = new Part(0, 0, 0);
    private final Part head = new Part(6, 6.5f, 0);
    private final Part[] fins = new Part[2];
    private final Part[] tailFins = new Part[2];
    private final Part ice = new Part(0, 0, 0);
    private final java.util.List<Part> parts = new java.util.ArrayList<>();
    private final Box headBox;
    private final Tex faceOpen, faceShut;
    private final boolean withIce;

    public SealModel(boolean withIce) {
        this.withIce = withIce;
        // body (lying)
        Box body = new Box(0, 3.5f, 0, 16, 7, 10);
        body.faces[PY] = px(noisy(16, 10, BACK, 0.018f, 3), SPOT, 3, 2, 4, 2, 9, 6, 12, 3);
        body.faces[NY] = noisy(16, 10, BELLY, 0.018f, 4);
        body.faces[PZ] = rows(noisy(16, 7, BODY, 0.018f, 5), BELLY, 5, 2);
        body.faces[NZ] = rows(noisy(16, 7, BODY, 0.018f, 6), BELLY, 5, 2);
        body.faces[PX] = rows(noisy(10, 7, BODY, 0.018f, 7), BELLY, 5, 2);
        body.faces[NX] = noisy(10, 7, BODY, 0.018f, 8);
        seal.boxes.add(body);

        Box tail = new Box(-10, 2, 0, 4, 4, 6);
        tail.faces[PY] = noisy(4, 6, BACK, 0.018f, 9);
        tail.faces[NY] = noisy(4, 6, BELLY, 0.018f, 10);
        for (int f : new int[] {PX, NX}) tail.faces[f] = rows(noisy(6, 4, BODY, 0.018f, 11), BELLY, 3, 1);
        for (int f : new int[] {PZ, NZ}) tail.faces[f] = rows(noisy(4, 4, BODY, 0.018f, 11), BELLY, 3, 1);
        seal.boxes.add(tail);

        // head (raised, turns around the neck)
        Tex faceBase = px(noisy(8, 8, BODY, 0.015f, 20), CHEEK, 0, 6, 7, 6);
        faceOpen = px(px(copy(faceBase), EYE, 1, 3, 2, 3, 1, 4, 2, 4, 5, 3, 6, 3, 5, 4, 6, 4), SHINE, 1, 3, 5, 3);
        faceShut = px(copy(faceBase), EYE, 1, 4, 2, 4, 5, 4, 6, 4);
        headBox = new Box(3, 2.5f, 0, 8, 8, 8);
        headBox.faces[PX] = faceOpen;
        headBox.faces[PY] = px(noisy(8, 8, BACK, 0.018f, 21), SPOT, 5, 2);
        headBox.faces[NY] = noisy(8, 8, BELLY, 0.018f, 22);
        for (int f : new int[] {NX, PZ, NZ}) headBox.faces[f] = noisy(8, 8, BODY, 0.018f, 23);
        head.boxes.add(headBox);
        Box snout = new Box(8.5f, 0, 0, 3, 3, 6);
        snout.faces[PX] = px(px(noisy(6, 3, SNOUT, 0.012f, 30), NOSE, 2, 0, 3, 0), WHISKER, 1, 1, 4, 1, 0, 2, 5, 2);
        for (int f = 1; f < 6; f++) {
            int tw = f == NX ? 6 : 3, th = (f == PY || f == NY) ? 6 : 3;
            snout.faces[f] = noisy(tw, th, SNOUT, 0.012f, 31);
        }
        head.boxes.add(snout);
        head.parent = seal;

        // fins (pivot at the root)
        for (int i = 0; i < 2; i++) {
            int s = i == 0 ? -1 : 1;
            Part fin = new Part(4, 1, s * 5);
            fin.boxes.add(finBox(1.5f, -0.5f, s, 40 + s));
            fin.ry = -s * 0.45f;
            fin.parent = seal;
            fins[i] = fin;
            Part tf = new Part(-12, 1, s * 1.2f);
            tf.boxes.add(finBox(-2, -0.5f, s, 50 + s));
            tf.ry = -s * 0.5f;
            tf.parent = seal;
            tailFins[i] = tf;
        }

        // ice block
        Box block = new Box(0, -1.5f, 0, 32, 3, 20);
        block.faces[PY] = px(noisy(32, 20, ICE, 0.015f, 60), ICE_LIGHT, 3, 3, 4, 3, 24, 5, 27, 14, 8, 15, 15, 2, 20, 17);
        block.faces[NY] = noisy(32, 20, ICE_DARK, 0.02f, 63);
        for (int f : new int[] {PX, NX}) block.faces[f] = rows(rows(noisy(20, 3, ICE_SIDE, 0.02f, 62), ICE_LIGHT, 0, 1), ICE_DARK, 2, 1);
        for (int f : new int[] {PZ, NZ}) block.faces[f] = rows(rows(noisy(32, 3, ICE_SIDE, 0.02f, 62), ICE_LIGHT, 0, 1), ICE_DARK, 2, 1);
        ice.boxes.add(block);

        parts.add(seal);
        parts.add(head);
        parts.add(fins[0]);
        parts.add(fins[1]);
        parts.add(tailFins[0]);
        parts.add(tailFins[1]);
        if (withIce) parts.add(ice);
    }

    private static Tex copy(Tex t) {
        return new Tex(t.w, t.h, t.rgb.clone());
    }

    private static Box finBox(float x, float y, int s, int seed) {
        Box b = new Box(x, y, s, 5, 1, 3);
        b.faces[PX] = noisy(3, 1, FLIPPER, 0.02f, seed);
        b.faces[NX] = noisy(3, 1, FLIPPER, 0.02f, seed);
        b.faces[PY] = noisy(5, 3, FLIPPER, 0.02f, seed);
        b.faces[NY] = noisy(5, 3, FLIPPER, 0.02f, seed);
        b.faces[PZ] = noisy(5, 1, FLIPPER, 0.02f, seed);
        b.faces[NZ] = noisy(5, 1, FLIPPER, 0.02f, seed);
        return b;
    }

    // ============================================================================================ pose

    private void apply(Pose p) {
        head.rz = p.headZ;
        head.ry = p.headY;
        fins[0].rx = -0.15f - p.finFlap * 0.55f;
        fins[1].rx = 0.15f + p.finFlap * 0.55f;
        tailFins[0].ry = 0.5f + p.tailWag * 0.35f;
        tailFins[1].ry = -0.5f - p.tailWag * 0.35f;
        seal.y = p.hop;
        headBox.faces[PX] = p.blink ? faceShut : faceOpen;
    }

    // ============================================================================================ framing

    /** Model-space bounds of the projected rest pose (with room for a hop), for sizing the buffer. */
    private float minX, maxX, minY, maxY;
    private boolean framed;

    private void frame() {
        if (framed) return;
        framed = true;
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
        Pose rest = new Pose();
        for (float hop = 0; hop <= 2.5f; hop += 2.5f) {
            rest.hop = hop;
            apply(rest);
            for (Part part : parts) {
                for (Box b : part.boxes) {
                    for (int i = 0; i < 8; i++) {
                        float[] v = {b.cx + ((i & 1) == 0 ? -b.w : b.w) / 2, b.cy + ((i & 2) == 0 ? -b.h : b.h) / 2, b.cz + ((i & 4) == 0 ? -b.d : b.d) / 2};
                        toWorld(part, v);
                        project(v, YAW);
                        minX = Math.min(minX, v[0]);
                        maxX = Math.max(maxX, v[0]);
                        minY = Math.min(minY, v[1]);
                        maxY = Math.max(maxY, v[1]);
                    }
                }
            }
        }
        // a little air for head turns / fin claps
        minX -= 1.5f;
        maxX += 1.5f;
        minY -= 1f;
        maxY += 1f;
    }

    /** Width of the model in units (projected). */
    public float width() {
        frame();
        return maxX - minX;
    }

    /** Height of the model in units (projected, screen down). */
    public float height() {
        frame();
        return maxY - minY;
    }

    public boolean withIce() {
        return withIce;
    }

    // ============================================================================================ transforms

    private static void rot(float[] v, float rx, float ry, float rz) {
        // three.js Euler 'XYZ': world = Rx * Ry * Rz * v
        float x = v[0], y = v[1], z = v[2];
        if (rz != 0) {
            float c = (float) Math.cos(rz), s = (float) Math.sin(rz);
            float nx = x * c - y * s;
            y = x * s + y * c;
            x = nx;
        }
        if (ry != 0) {
            float c = (float) Math.cos(ry), s = (float) Math.sin(ry);
            float nx = x * c + z * s;
            z = -x * s + z * c;
            x = nx;
        }
        if (rx != 0) {
            float c = (float) Math.cos(rx), s = (float) Math.sin(rx);
            float ny = y * c - z * s;
            z = y * s + z * c;
            y = ny;
        }
        v[0] = x;
        v[1] = y;
        v[2] = z;
    }

    /** Point in part space → model (world) space. */
    private static void toWorld(Part p, float[] v) {
        for (Part q = p; q != null; q = q.parent) {
            rot(v, q.rx, q.ry, q.rz);
            v[0] += q.x;
            v[1] += q.y;
            v[2] += q.z;
        }
    }

    /** Direction in part space → world space (no translation). */
    private static void dirToWorld(Part p, float[] v) {
        for (Part q = p; q != null; q = q.parent) rot(v, q.rx, q.ry, q.rz);
    }

    private static final float CP = (float) Math.cos(PITCH), SP = (float) Math.sin(PITCH);

    /** World → screen units (x right, y down) and depth (bigger = closer), in place. */
    private static void project(float[] v, float yaw) {
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        float x = v[0] * c + v[2] * s;
        float z = -v[0] * s + v[2] * c;
        float y = v[1];
        v[0] = x;
        v[1] = -(y * CP - z * SP);
        v[2] = y * SP + z * CP;
    }

    // ============================================================================================ raster

    private float[] depth = new float[0];

    /**
     * Renders the pose into {@code out} (ARGB, {@code w × h}, row major); {@code scale} = pixels per model unit.
     * The model is centred horizontally and bottom-aligned. Transparent where empty.
     */
    public void render(Pose pose, int[] out, int w, int h, float scale) {
        frame();
        apply(pose);
        if (depth.length < w * h) depth = new float[w * h];
        java.util.Arrays.fill(out, 0, w * h, 0);
        java.util.Arrays.fill(depth, 0, w * h, -Float.MAX_VALUE);
        float ox = w / 2f - (minX + maxX) / 2f * scale;
        float oy = h - maxY * scale;
        float[] o = new float[3], u = new float[3], v = new float[3], n = new float[3];
        for (Part part : parts) {
            for (Box b : part.boxes) {
                for (int f = 0; f < 6; f++) {
                    Tex t = b.faces[f];
                    if (t == null) continue;
                    faceGeometry(b, f, o, u, v, n);
                    toWorld(part, o);
                    dirToWorld(part, u);
                    dirToWorld(part, v);
                    dirToWorld(part, n);
                    float shade = shade(n, pose.yaw);
                    float rim = rim(n, pose.yaw);
                    project(o, pose.yaw);
                    projectDir(u, pose.yaw);
                    projectDir(v, pose.yaw);
                    projectDir(n, pose.yaw);
                    if (n[2] <= 0.001f) continue; // facing away
                    raster(t, o, u, v, shade, rim, out, w, h, scale, ox, oy);
                }
            }
        }
    }

    private static void projectDir(float[] v, float yaw) {
        project(v, yaw);
    }

    /** Lambert-ish light like the launcher (ambient + key light from (2,5,3)), lights fixed in the world. */
    private static float shade(float[] n, float yaw) {
        float[] w = {n[0], n[1], n[2]};
        rotY(w, yaw);
        float lx = 2, ly = 5, lz = 3, len = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        float d = Math.max(0, (w[0] * lx + w[1] * ly + w[2] * lz) / len);
        return Math.min(1f, 0.58f + 0.46f * d);
    }

    /** Purple rim light from behind-left (-3, 2, -3). */
    private static float rim(float[] n, float yaw) {
        float[] w = {n[0], n[1], n[2]};
        rotY(w, yaw);
        float lx = -3, ly = 2, lz = -3, len = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        return Math.max(0, (w[0] * lx + w[1] * ly + w[2] * lz) / len);
    }

    private static void rotY(float[] v, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float x = v[0] * c + v[2] * s;
        v[2] = -v[0] * s + v[2] * c;
        v[0] = x;
    }

    /**
     * Face corner {@code o} (texel 0,0 = top left of the canvas as seen from outside), full-length edge vectors
     * {@code u} (canvas x) and {@code v} (canvas y, downwards) and the outward normal, in part space. Matches the UV
     * layout of three.js' BoxGeometry, so the launcher's textures land the same way.
     */
    private static void faceGeometry(Box b, int f, float[] o, float[] u, float[] v, float[] n) {
        float hw = b.w / 2, hh = b.h / 2, hd = b.d / 2;
        float x = b.cx, y = b.cy, z = b.cz;
        switch (f) {
            case PX -> { set(o, x + hw, y + hh, z + hd); set(u, 0, 0, -b.d); set(v, 0, -b.h, 0); set(n, 1, 0, 0); }
            case NX -> { set(o, x - hw, y + hh, z - hd); set(u, 0, 0, b.d); set(v, 0, -b.h, 0); set(n, -1, 0, 0); }
            case PY -> { set(o, x - hw, y + hh, z - hd); set(u, b.w, 0, 0); set(v, 0, 0, b.d); set(n, 0, 1, 0); }
            case NY -> { set(o, x - hw, y - hh, z + hd); set(u, b.w, 0, 0); set(v, 0, 0, -b.d); set(n, 0, -1, 0); }
            case PZ -> { set(o, x - hw, y + hh, z + hd); set(u, b.w, 0, 0); set(v, 0, -b.h, 0); set(n, 0, 0, 1); }
            default -> { set(o, x + hw, y + hh, z - hd); set(u, -b.w, 0, 0); set(v, 0, -b.h, 0); set(n, 0, 0, -1); }
        }
    }

    private static void set(float[] a, float x, float y, float z) {
        a[0] = x;
        a[1] = y;
        a[2] = z;
    }

    private void raster(Tex t, float[] o, float[] u, float[] v, float shade, float rim, int[] out, int w, int h,
                        float scale, float ox, float oy) {
        float x0 = o[0] * scale + ox, y0 = o[1] * scale + oy;
        float ux = u[0] * scale, uy = u[1] * scale, vx = v[0] * scale, vy = v[1] * scale;
        float det = ux * vy - uy * vx;
        if (Math.abs(det) < 1e-6f) return;
        float minx = Math.min(Math.min(x0, x0 + ux), Math.min(x0 + vx, x0 + ux + vx));
        float maxx = Math.max(Math.max(x0, x0 + ux), Math.max(x0 + vx, x0 + ux + vx));
        float miny = Math.min(Math.min(y0, y0 + uy), Math.min(y0 + vy, y0 + uy + vy));
        float maxy = Math.max(Math.max(y0, y0 + uy), Math.max(y0 + vy, y0 + uy + vy));
        int px1 = Math.max(0, (int) Math.floor(minx)), px2 = Math.min(w - 1, (int) Math.ceil(maxx));
        int py1 = Math.max(0, (int) Math.floor(miny)), py2 = Math.min(h - 1, (int) Math.ceil(maxy));
        float inv = 1f / det;
        for (int py = py1; py <= py2; py++) {
            float sy = py + 0.5f - y0;
            for (int px = px1; px <= px2; px++) {
                float sx = px + 0.5f - x0;
                float a = (sx * vy - sy * vx) * inv;
                float bb = (ux * sy - uy * sx) * inv;
                if (a < 0 || a >= 1 || bb < 0 || bb >= 1) continue;
                float d = o[2] + a * u[2] + bb * v[2];
                int i = py * w + px;
                if (d <= depth[i]) continue;
                depth[i] = d;
                int tx = Math.min(t.w - 1, (int) (a * t.w));
                int ty = Math.min(t.h - 1, (int) (bb * t.h));
                int c = t.rgb[ty * t.w + tx];
                int r = (int) ((c >> 16 & 255) * shade + 0xb4 * rim * 0.16f);
                int g = (int) ((c >> 8 & 255) * shade + 0x8c * rim * 0.16f);
                int bl = (int) ((c & 255) * shade + 0xff * rim * 0.16f);
                out[i] = 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, bl);
            }
        }
    }
}
