package dev.henny.vibeyclient.module.servers.core;

public final class GapOffsets {
    private GapOffsets() {}

    public static int gap(int from, int slot) {
        return slot > from ? slot - 1 : slot;
    }

    public static int target(int index, int from, int slot, int rowHeight) {
        if (index == from) return 0;
        int without = index > from ? index - 1 : index;
        int place = without >= gap(from, slot) ? without + 1 : without;
        return (place - index) * rowHeight;
    }

    public static float approach(float current, float target, float dt) {
        float k = 1f - (float) Math.exp(-dt * 18f);
        float next = current + (target - current) * k;
        return Math.abs(target - next) < 0.25f ? target : next;
    }
}
