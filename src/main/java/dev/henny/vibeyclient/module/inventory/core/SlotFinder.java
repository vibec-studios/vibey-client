package dev.henny.vibeyclient.module.inventory.core;

import java.util.List;

final class SlotFinder {
    private SlotFinder() {}

    static SlotView byIndex(List<SlotView> slots, int index) {
        for (SlotView s : slots) if (s.index() == index) return s;
        return null;
    }

    static SlotView target(List<SlotView> slots, SlotView from, boolean hasContainer) {
        for (SlotView s : slots)
            if (s.section().opposite(from.section(), hasContainer) && s.holds(from.key()) && s.room() > 0) return s;
        SlotView fallback = null;
        for (SlotView s : slots) {
            if (!s.section().opposite(from.section(), hasContainer) || !s.empty() || s.room() <= 0) continue;
            if (s.section() != Section.HOTBAR) return s;
            if (fallback == null) fallback = s;
        }
        return fallback;
    }

    static SlotView source(List<SlotView> slots, SlotView to, boolean hasContainer) {
        SlotView best = null;
        for (SlotView s : slots) {
            if (!s.section().opposite(to.section(), hasContainer) || !s.holds(to.key())) continue;
            if (best == null || s.count() <= best.count()) best = s;
        }
        return best;
    }
}
