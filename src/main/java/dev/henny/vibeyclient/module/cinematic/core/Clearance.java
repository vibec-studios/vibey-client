package dev.henny.vibeyclient.module.cinematic.core;

public final class Clearance {
    public static final double MARGIN = 0.3;
    private static final double STEP = 0.25;

    private Clearance() {}

    public static boolean free(WorldView w, V3 p) {
        if (!p.finite() || p.y() < w.minY() + 1 || p.y() > w.maxY() - 1) return false;
        for (int i = 0; i < 8; i++) {
            double x = p.x() + ((i & 1) == 0 ? -MARGIN : MARGIN);
            double y = p.y() + ((i & 2) == 0 ? -MARGIN : MARGIN);
            double z = p.z() + ((i & 4) == 0 ? -MARGIN : MARGIN);
            int bx = floor(x), by = floor(y), bz = floor(z);
            if (!w.loaded(bx, bz)) return false;
            if ((w.tags(bx, by, bz) & (WorldView.SOLID | WorldView.WATER)) != 0) return false;
        }
        return true;
    }

    public static boolean segment(WorldView w, V3 a, V3 b) {
        double len = a.dist(b);
        int n = Math.max(1, (int) Math.ceil(len / STEP));
        for (int i = 0; i <= n; i++) if (!free(w, a.lerp(b, (double) i / n))) return false;
        return true;
    }

    public static boolean sight(WorldView w, V3 from, V3 to) {
        int x = floor(from.x()), y = floor(from.y()), z = floor(from.z());
        int ex = floor(to.x()), ey = floor(to.y()), ez = floor(to.z());
        V3 d = to.sub(from);
        int sx = (int) Math.signum(d.x()), sy = (int) Math.signum(d.y()), sz = (int) Math.signum(d.z());
        double tdx = sx == 0 ? Double.MAX_VALUE : Math.abs(1 / d.x());
        double tdy = sy == 0 ? Double.MAX_VALUE : Math.abs(1 / d.y());
        double tdz = sz == 0 ? Double.MAX_VALUE : Math.abs(1 / d.z());
        double tmx = sx == 0 ? Double.MAX_VALUE : boundary(from.x(), sx) * tdx;
        double tmy = sy == 0 ? Double.MAX_VALUE : boundary(from.y(), sy) * tdy;
        double tmz = sz == 0 ? Double.MAX_VALUE : boundary(from.z(), sz) * tdz;
        for (int guard = 0; guard < 2048; guard++) {
            if (x == ex && y == ey && z == ez) return true;
            if (Math.min(tmx, Math.min(tmy, tmz)) > 1) return true;
            if (tmx < tmy && tmx < tmz) {
                x += sx;
                tmx += tdx;
            } else if (tmy < tmz) {
                y += sy;
                tmy += tdy;
            } else {
                z += sz;
                tmz += tdz;
            }
            if (!w.loaded(x, z) || w.opaque(x, y, z)) return false;
        }
        return false;
    }

    public static boolean openSky(WorldView w, V3 p, int height) {
        int x = floor(p.x()), z = floor(p.z());
        int top = Math.min(w.maxY() - 1, floor(p.y()) + height);
        for (int y = floor(p.y()); y <= top; y++) if (w.opaque(x, y, z)) return false;
        return true;
    }

    private static double boundary(double v, int s) {
        double f = v - Math.floor(v);
        return s > 0 ? 1 - f : f;
    }

    public static int floor(double v) {
        return (int) Math.floor(v);
    }
}
