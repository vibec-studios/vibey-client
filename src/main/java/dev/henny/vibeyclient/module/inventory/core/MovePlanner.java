package dev.henny.vibeyclient.module.inventory.core;

import java.util.List;

public final class MovePlanner {
    private MovePlanner() {}

    public static List<Click> pushOne(List<SlotView> slots, int fromIndex, boolean hasContainer) {
        SlotView from = SlotFinder.byIndex(slots, fromIndex);
        if (from == null || from.empty() || from.section() == Section.NONE) return List.of();
        SlotView to = SlotFinder.target(slots, from, hasContainer);
        if (to == null) return List.of();
        return carryOne(from, to);
    }

    public static List<Click> pullOne(List<SlotView> slots, int toIndex, boolean hasContainer) {
        SlotView to = SlotFinder.byIndex(slots, toIndex);
        if (to == null || to.empty() || to.section() == Section.NONE || to.room() <= 0) return List.of();
        SlotView from = SlotFinder.source(slots, to, hasContainer);
        if (from == null) return List.of();
        return carryOne(from, to);
    }

    public static List<Click> pushStack(List<SlotView> slots, int fromIndex) {
        SlotView from = SlotFinder.byIndex(slots, fromIndex);
        if (from == null || from.empty() || from.section() == Section.NONE) return List.of();
        return List.of(Click.quickMove(from.index()));
    }

    public static List<Click> pullStack(List<SlotView> slots, int toIndex, boolean hasContainer) {
        SlotView to = SlotFinder.byIndex(slots, toIndex);
        if (to == null || to.empty() || to.section() == Section.NONE) return List.of();
        SlotView from = SlotFinder.source(slots, to, hasContainer);
        return from == null ? List.of() : List.of(Click.quickMove(from.index()));
    }

    private static List<Click> carryOne(SlotView from, SlotView to) {
        if (from.count() == 1) return List.of(Click.left(from.index()), Click.left(to.index()));
        return List.of(Click.left(from.index()), Click.right(to.index()), Click.left(from.index()));
    }
}
