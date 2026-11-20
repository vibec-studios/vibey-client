package dev.henny.companio.move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class PathFinder {
    public static final int MAX_NODES = 900;
    public static final int MAX_RANGE = 32;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private PathFinder() {}

    private record Node(int x, int y, int z, double g, double f, Node parent) {}

    public static List<int[]> find(Terrain t, int sx, int sy, int sz, int tx, int ty, int tz, double stopWithin, boolean canJump) {
        Map<Long, Double> best = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        Node start = new Node(sx, sy, sz, 0, h(sx, sy, sz, tx, ty, tz), null);
        open.add(start);
        best.put(key(sx, sy, sz), 0.0);
        Node closest = start;
        int expanded = 0;
        while (!open.isEmpty() && expanded < MAX_NODES) {
            Node n = open.poll();
            if (n.g > best.getOrDefault(key(n.x, n.y, n.z), Double.MAX_VALUE) + 1e-9) continue;
            expanded++;
            double hn = h(n.x, n.y, n.z, tx, ty, tz);
            if (hn < h(closest.x, closest.y, closest.z, tx, ty, tz)) closest = n;
            if (hn <= stopWithin) return path(n);
            for (int[] d : DIRS) expand(t, n, d, tx, ty, tz, sx, sz, canJump, best, open);
        }
        return closest == start ? List.of() : path(closest);
    }

    private static void expand(Terrain t, Node n, int[] d, int tx, int ty, int tz, int sx, int sz, boolean canJump,
                               Map<Long, Double> best, PriorityQueue<Node> open) {
        int x = n.x + d[0], z = n.z + d[1];
        if (Math.abs(x - sx) > MAX_RANGE || Math.abs(z - sz) > MAX_RANGE) return;
        boolean diag = d[0] != 0 && d[1] != 0;
        if (diag && (!t.clear(n.x + d[0], n.y, n.z) || !t.clear(n.x, n.y, n.z + d[1]))) return;
        int y = Integer.MIN_VALUE;
        double extra = 0;
        if (t.standable(x, n.y, z)) y = n.y;
        else if (canJump && t.standable(x, n.y + 1, z) && t.clear(n.x, n.y + 1, n.z)) {
            y = n.y + 1;
            extra = 0.8;
        } else if (t.clear(x, n.y, z)) {
            y = t.dropTo(x, n.y - 1, z);
            extra = 0.4 * (n.y - y);
        }
        if (y == Integer.MIN_VALUE) return;
        if (t.water(x, y, z)) extra += 1.5;
        double g = n.g + (diag ? 1.414 : 1.0) + extra;
        long k = key(x, y, z);
        if (g >= best.getOrDefault(k, Double.MAX_VALUE)) return;
        best.put(k, g);
        open.add(new Node(x, y, z, g, g + h(x, y, z, tx, ty, tz), n));
    }

    private static double h(int x, int y, int z, int tx, int ty, int tz) {
        double dx = tx - x, dy = (ty - y) * 1.5, dz = tz - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static List<int[]> path(Node n) {
        List<int[]> out = new ArrayList<>();
        for (Node c = n; c != null; c = c.parent) out.add(new int[] {c.x, c.y, c.z});
        Collections.reverse(out);
        if (!out.isEmpty()) out.remove(0);
        return out;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }
}
