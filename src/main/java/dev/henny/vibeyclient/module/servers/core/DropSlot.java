package dev.henny.vibeyclient.module.servers.core;

public final class DropSlot {
    private DropSlot() {}

    public static int of(int[] tops, int[] heights, double y) {
        for (int i = 0; i < tops.length; i++) if (y < tops[i] + heights[i] / 2.0) return i;
        return tops.length;
    }

    public static int clamp(int slot, int lo, int hi) {
        return Math.max(lo, Math.min(hi, slot));
    }

    public static boolean noop(int from, int slot) {
        return slot == from || slot == from + 1;
    }

    public static int[] group(boolean[] marked, int from) {
        int lo = from, hi = from + 1;
        while (lo > 0 && marked[lo - 1] == marked[from]) lo--;
        while (hi < marked.length && marked[hi] == marked[from]) hi++;
        return new int[] {lo, hi};
    }
}
