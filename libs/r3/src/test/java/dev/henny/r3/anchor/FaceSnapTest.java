package dev.henny.r3.anchor;

import dev.henny.r3.math.Face;
import dev.henny.r3.object.Transform;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FaceSnapTest {
    @Test
    void wallScreensStandUpright() {
        assertEquals(Face.UP, FaceSnap.upFor(Face.NORTH, 0, 0));
        assertEquals(Face.UP, FaceSnap.upFor(Face.EAST, 123, 3));
    }

    @Test
    void floorScreenTopPointsAwayFromViewer() {
        assertEquals(Face.SOUTH, FaceSnap.upFor(Face.UP, 0, 0));
        assertEquals(Face.NORTH, FaceSnap.upFor(Face.UP, 180, 0));
    }

    @Test
    void ceilingScreenIsReadableFromBelow() {
        assertEquals(Face.NORTH, FaceSnap.upFor(Face.DOWN, 0, 0));
    }

    @Test
    void rotateInQuarterSteps() {
        assertEquals(Face.WEST, FaceSnap.upFor(Face.UP, 0, 1));
        assertEquals(Face.NORTH, FaceSnap.upFor(Face.UP, 0, 2));
        assertEquals(Face.EAST, FaceSnap.upFor(Face.UP, 0, 3));
        assertEquals(Face.SOUTH, FaceSnap.upFor(Face.UP, 0, 4));
    }

    @Test
    void orientationMatchesFaceAndUp() {
        Transform t = new Transform();
        FaceSnap.orientation(Face.EAST, Face.UP, t.orientation());
        Vector3d n = t.normal(new Vector3d());
        Vector3d u = t.up(new Vector3d());
        assertEquals(1, n.x, 1e-6);
        assertEquals(1, u.y, 1e-6);
        Vector3d r = t.right(new Vector3d());
        assertEquals(1, new Vector3d(r).cross(u).dot(n), 1e-6);
    }

    @Test
    void poseSnapsToEighthsAndOffsetsFromTheFace() {
        Transform t = new Transform();
        FaceSnap.pose(new Vector3d(0.43, 65.07, 5.0), Face.NORTH, Face.UP, 0.02, 0.125, t);
        assertEquals(0.375, t.position().x, 1e-9);
        assertEquals(65.125, t.position().y, 1e-9);
        assertEquals(4.98, t.position().z, 1e-9);
    }

    @Test
    void floorPoseKeepsHeightAndLiftsAboveFloor() {
        Transform t = new Transform();
        FaceSnap.pose(new Vector3d(2.31, 64.0, -1.06), Face.UP, Face.SOUTH, 0.02, 0.125, t);
        assertEquals(64.02, t.position().y, 1e-9);
        assertEquals(2.25, t.position().x, 1e-9);
        assertEquals(-1.0, t.position().z, 1e-9);
        assertEquals(1, t.normal(new Vector3d()).y, 1e-6);
    }

    @Test
    void blockFaceAnchorRejectsParallelUp() {
        assertThrows(IllegalArgumentException.class, () -> new BlockFaceAnchor(Face.UP, Face.DOWN, 0, 0, 0));
    }
}
