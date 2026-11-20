package dev.henny.vibeyclient.module.servers.core;

public final class MoveTarget {
    private MoveTarget() {}

    public static int finalIndex(int[] real, int from, int slot, int lo, int hi) {
        int rf = real[from];
        if (slot < hi) {
            int anchor = real[slot];
            return anchor > rf ? anchor - 1 : anchor;
        }
        int anchor = real[hi - 1];
        return anchor >= rf ? anchor : anchor + 1;
    }

    public static int finalIndex(int[] real, int from, int slot) {
        return finalIndex(real, from, slot, 0, real.length);
    }
}
