package dev.henny.vibeyclient.module.screens.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapTest {
    @Test
    void snapsToScreenCentreWithAGuide() {
        Snap.Result r = Snap.move(new Box(152, 50, 100, 20), List.of(), 400, 300);
        assertEquals(150, r.x(), 0.01);
        assertEquals(1, r.guides().size());
        assertTrue(r.guides().get(0).vertical());
        assertEquals(200, r.guides().get(0).at(), 0.01);
    }

    @Test
    void snapsToNeighbourLeftEdgeAndBelowGap() {
        Box other = new Box(40, 40, 100, 20);
        Snap.Result r = Snap.move(new Box(42, 62, 60, 20), List.of(other), 400, 300);
        assertEquals(40, r.x(), 0.01);
        assertEquals(64, r.y(), 0.01);
    }

    @Test
    void staysFreeOutsideTheRange() {
        Snap.Result r = Snap.move(new Box(70, 77, 30, 20), List.of(new Box(10, 10, 20, 20)), 400, 300);
        assertEquals(70, r.x(), 0.01);
        assertEquals(77, r.y(), 0.01);
        assertTrue(r.guides().isEmpty());
    }

    @Test
    void snapsToTheScreenMargin() {
        Snap.Result r = Snap.move(new Box(2, 150, 30, 20), List.of(), 400, 300);
        assertEquals(Snap.MARGIN, r.x(), 0.01);
        Snap.Result far = Snap.move(new Box(368, 150, 30, 20), List.of(), 400, 300);
        assertEquals(400 - Snap.MARGIN - 30, far.x(), 0.01);
    }

    @Test
    void edgeSnapsToNeighbourRightEdge() {
        Snap.Result r = Snap.edge(138, true, new Box(10, 80, 128, 20), List.of(new Box(40, 40, 100, 20)), 400, 300);
        assertEquals(140, r.x(), 0.01);
        assertEquals(1, r.guides().size());
    }
}
