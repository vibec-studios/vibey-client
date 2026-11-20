package dev.henny.vibeyclient.module.inventory.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovePlannerTest {
    private static SlotView slot(int i, Section s, String key, int count) {
        return new SlotView(i, s, key, count, 64, true);
    }

    private static List<SlotView> chest() {
        return List.of(
                slot(0, Section.CONTAINER, "stone", 10),
                slot(1, Section.CONTAINER, "", 0),
                slot(2, Section.CONTAINER, "dirt", 1),
                slot(27, Section.MAIN, "", 0),
                slot(28, Section.MAIN, "stone", 64),
                slot(29, Section.MAIN, "stone", 5),
                slot(54, Section.HOTBAR, "", 0));
    }

    @Test
    void pushOneUsesAMatchingStackFirst() {
        assertEquals(List.of(Click.left(0), Click.right(29), Click.left(0)), MovePlanner.pushOne(chest(), 0, true));
    }

    @Test
    void pushOneOfASingleItemTakesTwoClicks() {
        assertEquals(List.of(Click.left(2), Click.left(27)), MovePlanner.pushOne(chest(), 2, true));
    }

    @Test
    void pushPrefersMainOverHotbarForEmptySlots() {
        List<SlotView> slots = List.of(slot(0, Section.CONTAINER, "gold", 3), slot(54, Section.HOTBAR, "", 0),
                slot(30, Section.MAIN, "", 0));
        assertEquals(Click.right(30), MovePlanner.pushOne(slots, 0, true).get(1));
    }

    @Test
    void pullOneTakesFromTheSmallestStack() {
        List<Click> plan = MovePlanner.pullOne(chest(), 0, true);
        assertEquals(List.of(Click.left(29), Click.right(0), Click.left(29)), plan);
    }

    @Test
    void nothingToDoGivesAnEmptyPlan() {
        assertTrue(MovePlanner.pushOne(chest(), 1, true).isEmpty());
        assertTrue(MovePlanner.pullOne(chest(), 2, true).isEmpty());
        List<SlotView> full = List.of(slot(0, Section.CONTAINER, "stone", 3), new SlotView(27, Section.MAIN, "dirt", 64, 64, true));
        assertTrue(MovePlanner.pushOne(full, 0, true).isEmpty());
        assertTrue(MovePlanner.pushOne(List.of(slot(5, Section.NONE, "stone", 1)), 5, true).isEmpty());
    }

    @Test
    void playerInventoryMovesBetweenMainAndHotbar() {
        List<SlotView> inv = List.of(slot(9, Section.MAIN, "arrow", 20), slot(10, Section.MAIN, "", 0), slot(36, Section.HOTBAR, "", 0));
        assertEquals(List.of(Click.left(9), Click.right(36), Click.left(9)), MovePlanner.pushOne(inv, 9, false));
    }

    @Test
    void wholeStacksUseQuickMove() {
        assertEquals(List.of(Click.quickMove(0)), MovePlanner.pushStack(chest(), 0));
        assertEquals(List.of(Click.quickMove(29)), MovePlanner.pullStack(chest(), 0, true));
    }

    @Test
    void slotsThatRefuseTheItemAreNoTarget() {
        List<SlotView> slots = List.of(slot(0, Section.CONTAINER, "stone", 3), new SlotView(27, Section.MAIN, "", 0, 64, false),
                slot(28, Section.MAIN, "", 0));
        assertEquals(Click.right(28), MovePlanner.pushOne(slots, 0, true).get(1));
    }
}
