package dev.henny.vibeyclient.module.inventory.core;

import java.util.HashSet;
import java.util.Set;

public final class DragTracker {
    private final Set<Integer> visited = new HashSet<>();
    private boolean active;
    private String matchKey;

    public void start(int slot, String key, boolean matching) {
        visited.clear();
        visited.add(slot);
        active = true;
        matchKey = matching && key != null && !key.isEmpty() ? key : null;
    }

    public boolean active() {
        return active;
    }

    public boolean visit(SlotView slot) {
        if (!active || slot == null || slot.empty() || slot.section() == Section.NONE) return false;
        if (matchKey != null && !matchKey.equals(slot.key())) return false;
        return visited.add(slot.index());
    }

    public void stop() {
        active = false;
        visited.clear();
        matchKey = null;
    }
}
