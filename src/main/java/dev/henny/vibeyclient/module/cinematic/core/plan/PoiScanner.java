package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.WorldView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PoiScanner {
    static final int CELL = 4;
    static final int DEPTH = 6;
    static final int KEEP = 12;
    static final double SPACING = 7;

    private PoiScanner() {}

    public static List<Poi> scan(WorldView w, V3 center, double radius) {
        int r = (int) Math.max(CELL, radius);
        int cx0 = (int) Math.floor(center.x()), cz0 = (int) Math.floor(center.z());
        List<Poi> all = new ArrayList<>();
        for (int dx = -r; dx <= r; dx += CELL) {
            for (int dz = -r; dz <= r; dz += CELL) {
                if (dx * dx + dz * dz > r * r) continue;
                Poi p = cell(w, cx0 + dx, cz0 + dz);
                if (p != null && p.score() > 0) all.add(p);
            }
        }
        all.sort(Comparator.comparingDouble(Poi::score).reversed());
        List<Poi> out = new ArrayList<>();
        for (Poi p : all) {
            if (out.size() >= KEEP) break;
            boolean near = false;
            for (Poi o : out) if (o.focus().dist(p.focus()) < SPACING) near = true;
            if (!near) out.add(p);
        }
        return out;
    }

    static Poi cell(WorldView w, int x0, int z0) {
        int built = 0, lights = 0, waterCols = 0, dryCols = 0, minS = Integer.MAX_VALUE, maxS = Integer.MIN_VALUE, sum = 0, cols = 0;
        int[] kinds = new int[24];
        int nk = 0;
        for (int x = x0; x < x0 + CELL; x++) {
            for (int z = z0; z < z0 + CELL; z++) {
                if (!w.loaded(x, z)) return null;
                int s = w.surface(x, z);
                if (s <= w.minY()) continue;
                cols++;
                sum += s;
                minS = Math.min(minS, s);
                maxS = Math.max(maxS, s);
                if ((w.tags(x, s, z) & WorldView.WATER) != 0) waterCols++;
                else dryCols++;
                for (int y = s - DEPTH; y <= s; y++) {
                    int t = w.tags(x, y, z);
                    if ((t & WorldView.BUILT) != 0) built++;
                    if ((t & WorldView.LIGHT) != 0) lights++;
                    if ((t & WorldView.SOLID) != 0 && nk < kinds.length) nk = addKind(kinds, nk, w.kind(x, y, z));
                }
            }
        }
        if (cols == 0) return null;
        int relief = Math.min(12, maxS - minS);
        boolean water = waterCols > 0 && dryCols > 0;
        double score = built + lights * 2.5 + Math.max(0, nk - 2) * 1.5 + (water ? 4 + Math.min(waterCols, dryCols) * 0.5 : 0) + relief * 0.6;
        V3 focus = new V3(x0 + CELL / 2.0, (double) sum / cols + 1.5, z0 + CELL / 2.0);
        return new Poi(focus, score, built, lights, nk, relief, water);
    }

    private static int addKind(int[] kinds, int n, int k) {
        for (int i = 0; i < n; i++) if (kinds[i] == k) return n;
        kinds[n] = k;
        return n + 1;
    }
}
