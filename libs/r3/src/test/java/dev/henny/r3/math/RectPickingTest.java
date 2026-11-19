package dev.henny.r3.math;

import dev.henny.r3.object.Transform;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RectPickingTest {
    private static final double EPS = 1e-6;

    private OrientedRect rectFacingMinusZ() {
        Transform t = new Transform().size(4f, 2f).position(0, 0, 10);
        Axes.facing(0, 0, -1, t.orientation());
        return new OrientedRect().set(t);
    }

    @Test
    void centerHitGivesMiddleOfPanel() {
        RectHit hit = new RectHit();
        assertTrue(rectFacingMinusZ().hit(new Ray().set(0, 0, 0, 0, 0, 1), 0, false, 100, hit));
        assertEquals(10, hit.distance(), EPS);
        assertEquals(0.5, hit.u(), EPS);
        assertEquals(0.5, hit.v(), EPS);
        assertEquals(160f, hit.pixelX(320), 1e-3);
        assertEquals(90f, hit.pixelY(180), 1e-3);
        assertTrue(hit.front());
    }

    @Test
    void topLeftOfPanelIsPixelZero() {
        OrientedRect rect = rectFacingMinusZ();
        RectHit hit = new RectHit();
        double leftX = rect.right().x() < 0 ? 1.999 : -1.999;
        assertTrue(rect.hit(new Ray().set(leftX, 0.999, 0, 0, 0, 1), 0, false, 100, hit));
        assertEquals(0.0, hit.u(), 1e-3);
        assertEquals(0.0, hit.v(), 1e-3);
    }

    @Test
    void viewerRightIsPanelRight() {
        OrientedRect rect = rectFacingMinusZ();
        assertEquals(-1, rect.right().x(), EPS);
        RectHit hit = new RectHit();
        assertTrue(rect.hit(new Ray().set(-1, 0, 0, 0, 0, 1), 0, false, 100, hit));
        assertEquals(0.75, hit.u(), EPS);
    }

    @Test
    void backFaceOnlyWhenDoubleSided() {
        OrientedRect rect = rectFacingMinusZ();
        RectHit hit = new RectHit();
        Ray fromBehind = new Ray().set(0, 0, 20, 0, 0, -1);
        assertFalse(rect.hit(fromBehind, 0, false, 100, hit));
        assertTrue(rect.hit(fromBehind, 0, true, 100, hit));
        assertFalse(hit.front());
    }

    @Test
    void missesOutsideAndBeyondDistanceAndBehindOrigin() {
        OrientedRect rect = rectFacingMinusZ();
        RectHit hit = new RectHit();
        assertFalse(rect.hit(new Ray().set(2.5, 0, 0, 0, 0, 1), 0, false, 100, hit));
        assertTrue(rect.hit(new Ray().set(2.5, 0, 0, 0, 0, 1), 0.6, false, 100, hit));
        assertFalse(hit.inside());
        assertFalse(rect.hit(new Ray().set(0, 0, 0, 0, 0, 1), 0, false, 9.5, hit));
        assertFalse(rect.hit(new Ray().set(0, 0, 0, 1, 0, 0), 0, false, 100, hit));
    }

    @Test
    void planeHitWorksOutsideTheRect() {
        RectHit hit = new RectHit();
        assertTrue(rectFacingMinusZ().planeHit(new Ray().set(-5, 3, 0, 0, 0, 1), hit));
        assertEquals(5, hit.localX(), EPS);
        assertEquals(3, hit.localY(), EPS);
    }

    @Test
    void rotatedPanelUsesItsOwnAxes() {
        Transform t = new Transform().size(2f, 2f).position(5, 0, 0);
        t.orientation().set(new Quaternionf().rotationY((float) Math.toRadians(-90)));
        OrientedRect rect = new OrientedRect().set(t);
        assertEquals(-1, rect.normal().x(), 1e-6);
        RectHit hit = new RectHit();
        assertTrue(rect.hit(new Ray().set(0, 0.5, 0, 1, 0, 0), 0, false, 100, hit));
        assertEquals(5, hit.distance(), EPS);
        assertEquals(0.25, hit.v(), EPS);
    }

    @Test
    void orientedBoxIntersection() {
        OrientedBox box = new OrientedBox().set(new org.joml.Vector3d(0, 0, 5), new Quaternionf(), 1, 1, 1);
        assertEquals(4, box.intersect(new Ray().set(0, 0, 0, 0, 0, 1)), EPS);
        assertTrue(Double.isNaN(box.intersect(new Ray().set(3, 0, 0, 0, 0, 1))));
        assertTrue(box.contains(new org.joml.Vector3d(0.5, 0.5, 5.5)));
        assertFalse(box.contains(new org.joml.Vector3d(0, 1.5, 5)));
    }

    @Test
    void planeIntersection() {
        Plane p = new Plane().set(new org.joml.Vector3d(0, 2, 0), new org.joml.Vector3d(0, 1, 0));
        assertEquals(3, p.intersect(new Ray().set(0, 5, 0, 0, -1, 0)), EPS);
        assertTrue(Double.isNaN(p.intersect(new Ray().set(0, 5, 0, 0, 1, 0))));
        assertEquals(3, p.signedDistance(new org.joml.Vector3d(1, 5, 1)), EPS);
    }
}
