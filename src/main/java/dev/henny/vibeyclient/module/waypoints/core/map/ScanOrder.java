package dev.henny.vibeyclient.module.waypoints.core.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScanOrder {
    private final int radius;
    private final int[] dx;
    private final int[] dz;

    public ScanOrder(int radius) {
        this.radius = Math.max(0, radius);
        List<int[]> all = new ArrayList<>();
        for (int z = -this.radius; z <= this.radius; z++) for (int x = -this.radius; x <= this.radius; x++) all.add(new int[] {x, z});
        all.sort(Comparator.comparingInt((int[] p) -> p[0] * p[0] + p[1] * p[1]).thenComparingInt(p -> p[1]).thenComparingInt(p -> p[0]));
        dx = new int[all.size()];
        dz = new int[all.size()];
        for (int i = 0; i < all.size(); i++) {
            dx[i] = all.get(i)[0];
            dz[i] = all.get(i)[1];
        }
    }

    public int radius() {
        return radius;
    }

    public int size() {
        return dx.length;
    }

    public int dx(int i) {
        return dx[i];
    }

    public int dz(int i) {
        return dz[i];
    }

    public static boolean due(long scannedAt, long now, int ring) {
        if (scannedAt < 0) return true;
        long age = now - scannedAt;
        return ring <= 1 ? age >= 40 : ring <= 4 ? age >= 400 : age >= 2400;
    }
}
