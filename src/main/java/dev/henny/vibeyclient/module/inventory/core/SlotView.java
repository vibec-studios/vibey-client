package dev.henny.vibeyclient.module.inventory.core;

public record SlotView(int index, Section section, String key, int count, int max, boolean accepts) {
    public static final String EMPTY = "";

    public boolean empty() {
        return count <= 0 || key.isEmpty();
    }

    public boolean holds(String other) {
        return !empty() && key.equals(other);
    }

    public int room() {
        if (!accepts) return 0;
        return Math.max(0, max - (empty() ? 0 : count));
    }
}
