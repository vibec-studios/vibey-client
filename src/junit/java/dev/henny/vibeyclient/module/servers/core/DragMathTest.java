package dev.henny.vibeyclient.module.servers.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragMathTest {
    @Test
    void trackerStartsAfterThresholdAndClickStaysClick() {
        DragTracker t = new DragTracker();
        t.press(2, 10, 10);
        assertFalse(t.move(11, 12));
        assertEquals(-1, t.release());
        t.press(2, 10, 10);
        assertTrue(t.move(10, 20));
        assertTrue(t.dragging());
        assertEquals(2, t.release());
        assertFalse(t.pressed());
        t.press(1, 0, 0);
        t.move(0, 30);
        t.cancel();
        assertEquals(-1, t.release());
    }

    @Test
    void dropSlotUsesRowMiddles() {
        int[] tops = {0, 36, 72}, h = {36, 36, 36};
        assertEquals(0, DropSlot.of(tops, h, 10));
        assertEquals(1, DropSlot.of(tops, h, 20));
        assertEquals(2, DropSlot.of(tops, h, 60));
        assertEquals(3, DropSlot.of(tops, h, 100));
        assertTrue(DropSlot.noop(1, 1));
        assertTrue(DropSlot.noop(1, 2));
        assertFalse(DropSlot.noop(1, 0));
    }

    @Test
    void groupsKeepFavouritesTogether() {
        boolean[] fav = {true, true, false, false, false};
        assertArrayEquals(new int[] {0, 2}, DropSlot.group(fav, 1));
        assertArrayEquals(new int[] {2, 5}, DropSlot.group(fav, 3));
        assertEquals(2, DropSlot.clamp(0, 2, 5));
    }

    @Test
    void moveTargetPlainList() {
        int[] real = {0, 1, 2, 3, 4};
        assertEquals(0, MoveTarget.finalIndex(real, 2, 0));
        assertEquals(3, MoveTarget.finalIndex(real, 1, 4));
        assertEquals(4, MoveTarget.finalIndex(real, 0, 5));
    }

    @Test
    void moveTargetFilteredListLandsBeforeShownNeighbour() {
        List<String> servers = new ArrayList<>(List.of("a", "b", "c", "d", "e", "f"));
        int[] shown = {1, 3, 5};
        int to = MoveTarget.finalIndex(shown, 2, 0);
        ListMove.move(servers, shown[2], to);
        assertEquals(List.of("a", "f", "b", "c", "d", "e"), servers);
        servers = new ArrayList<>(List.of("a", "b", "c", "d", "e", "f"));
        ListMove.move(servers, shown[0], MoveTarget.finalIndex(shown, 0, 3));
        assertEquals(List.of("a", "c", "d", "e", "f", "b"), servers);
    }

    @Test
    void moveTargetFavouritesFirstStaysInGroup() {
        List<String> servers = new ArrayList<>(List.of("a", "B", "c", "D"));
        int[] shown = {1, 3, 0, 2};
        ListMove.move(servers, shown[1], MoveTarget.finalIndex(shown, 1, 0, 0, 2));
        assertEquals(List.of("a", "D", "B", "c"), servers);
        servers = new ArrayList<>(List.of("a", "B", "c", "D"));
        ListMove.move(servers, shown[0], MoveTarget.finalIndex(shown, 0, 2, 0, 2));
        assertEquals(List.of("a", "c", "D", "B"), servers);
    }

    @Test
    void swapsMatchMove() {
        List<Integer> a = new ArrayList<>(List.of(0, 1, 2, 3, 4));
        List<Integer> b = new ArrayList<>(a);
        ListMove.move(a, 3, 0);
        ListMove.bySwaps(3, 0, (i, j) -> b.set(i, b.set(j, b.get(i))));
        assertEquals(a, b);
        ListMove.move(a, 0, 4);
        ListMove.bySwaps(0, 4, (i, j) -> b.set(i, b.set(j, b.get(i))));
        assertEquals(a, b);
    }

    @Test
    void gapOffsetsOpenRoomAtTheSlot() {
        assertEquals(36, GapOffsets.target(0, 2, 0, 36));
        assertEquals(36, GapOffsets.target(1, 2, 0, 36));
        assertEquals(0, GapOffsets.target(3, 2, 0, 36));
        assertEquals(-36, GapOffsets.target(1, 0, 3, 36));
        assertEquals(0, GapOffsets.target(3, 0, 3, 36));
        assertEquals(0, GapOffsets.target(1, 2, 2, 36));
        assertEquals(10f, GapOffsets.approach(9.9f, 10f, 0.016f));
    }

    @Test
    void autoScrollNearEdgesOnly() {
        assertTrue(AutoScroll.step(35, 33, 300) < 0);
        assertTrue(AutoScroll.step(298, 33, 300) > 0);
        assertEquals(0f, AutoScroll.step(150, 33, 300));
        assertEquals(-AutoScroll.MAX, AutoScroll.step(0, 33, 300));
    }
}
