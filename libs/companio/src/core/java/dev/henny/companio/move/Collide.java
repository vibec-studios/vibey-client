package dev.henny.companio.move;

final class Collide {
    static final double EPS = 1e-4;

    private Collide() {}

    static boolean blocked(BlockView v, double x, double y, double z, double halfW, double h) {
        int x0 = floor(x - halfW), x1 = floor(x + halfW - EPS);
        int z0 = floor(z - halfW), z1 = floor(z + halfW - EPS);
        int y0 = floor(y + EPS) - 1, y1 = floor(y + h - EPS);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                for (int cy = y0; cy <= y1; cy++) {
                    float top = v.top(cx, cy, cz);
                    if (top <= 0) continue;
                    double bottom = cy, ceil = cy + top;
                    if (ceil > y + EPS && bottom < y + h - EPS) return true;
                }
            }
        }
        return false;
    }

    static double ground(BlockView v, double x, double fromY, double toY, double z, double halfW) {
        int x0 = floor(x - halfW), x1 = floor(x + halfW - EPS);
        int z0 = floor(z - halfW), z1 = floor(z + halfW - EPS);
        double best = Double.NEGATIVE_INFINITY;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                for (int cy = floor(fromY + EPS); cy >= floor(toY) - 1; cy--) {
                    float top = v.top(cx, cy, cz);
                    if (top <= 0) continue;
                    double h = cy + top;
                    if (h <= fromY + EPS && h >= toY - EPS && h > best) best = h;
                }
            }
        }
        return best;
    }

    static double obstacleTop(BlockView v, double x, double y, double z, double halfW, double h) {
        int x0 = floor(x - halfW), x1 = floor(x + halfW - EPS);
        int z0 = floor(z - halfW), z1 = floor(z + halfW - EPS);
        double best = y;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                for (int cy = floor(y + EPS); cy <= floor(y + h - EPS); cy++) {
                    float top = v.top(cx, cy, cz);
                    if (top > 0 && cy + top > best) best = cy + top;
                }
            }
        }
        return best;
    }

    static int floor(double d) {
        return (int) Math.floor(d);
    }
}
