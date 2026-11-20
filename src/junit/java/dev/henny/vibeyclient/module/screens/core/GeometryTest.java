package dev.henny.vibeyclient.module.screens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryTest {
    @Test
    void anchorsRoundTripForEveryThird() {
        float w = 400, h = 300;
        for (Box b : new Box[] {new Box(4, 4, 40, 20), new Box(180, 140, 40, 20), new Box(356, 276, 40, 20)}) {
            Pos p = Pos.of(b, w, h);
            Box back = p.resolve(b.w(), b.h(), w, h);
            assertTrue(back.same(b), b + " -> " + back);
        }
    }

    @Test
    void rightAnchorFollowsTheRightEdgeOnResize() {
        Pos p = Pos.of(new Box(356, 10, 40, 20), 400, 300);
        assertEquals(2, p.ax());
        Box wider = p.resolve(40, 20, 600, 300);
        assertEquals(556, wider.x(), 0.01);
    }

    @Test
    void centerAnchorStaysCentred() {
        Pos p = Pos.of(new Box(150, 100, 100, 20), 400, 300);
        assertEquals(1, p.ax());
        assertEquals(250, p.resolve(100, 20, 600, 300).x(), 0.01);
    }

    @Test
    void scaleWithoutMoveGrowsAroundTheCentre() {
        Box base = new Box(100, 100, 100, 20);
        Box v = Geometry.place(base, Placement.DEFAULT.withScale(2f), 400, 300, true);
        assertEquals(new Box(50, 90, 200, 40), v);
        Box flat = Geometry.place(base, Placement.DEFAULT.withScale(2f), 400, 300, false);
        assertEquals(base, flat);
    }

    @Test
    void resizeFromLeftKeepsRightEdge() {
        Box v = new Box(100, 100, 100, 20);
        Placement p = Geometry.resized(Placement.DEFAULT, v, 140, true, 400, 300, true);
        assertEquals(140, p.width());
        Box placed = Geometry.place(new Box(0, 0, 100, 20), p, 400, 300, true);
        assertEquals(200, placed.right(), 0.01);
        assertEquals(60, placed.x(), 0.01);
    }

    @Test
    void resizeRespectsScaleAndMinimum() {
        Placement s2 = Placement.DEFAULT.withScale(2f);
        Placement p = Geometry.resized(s2, new Box(0, 0, 200, 40), 300, false, 400, 300, true);
        assertEquals(150, p.width());
        assertEquals(Geometry.MIN_WIDTH, Geometry.resized(s2, new Box(0, 0, 200, 40), 1, false, 400, 300, true).width());
    }

    @Test
    void scaledKeepsTheCentreOfAMovedWidget() {
        Placement moved = Geometry.moved(Placement.DEFAULT, new Box(10, 10, 100, 20), 400, 300);
        Placement p = Geometry.scaled(moved, new Box(10, 10, 100, 20), 2f, 400, 300);
        Box v = Geometry.place(new Box(0, 0, 100, 20), p, 400, 300, true);
        assertEquals(60, v.cx(), 0.6);
        assertEquals(20, v.cy(), 0.6);
        assertNull(Geometry.scaled(Placement.DEFAULT, new Box(0, 0, 10, 10), 2f, 400, 300).pos());
    }

    @Test
    void placementClampsScale() {
        assertEquals(Placement.MAX_SCALE, Placement.DEFAULT.withScale(99f).scale());
        assertEquals(1f, Placement.DEFAULT.withScale(Float.NaN).scale());
        assertEquals(ScreenLayout.MIN_SCALE, ScreenLayout.EMPTY.withScale(0.1f).scale());
    }
}
