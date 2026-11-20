package dev.henny.vibeyclient.module.visuals.items.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GesturesTest {
    private static final float W = 640, H = 360;

    @Test
    void dragFollowsThePointerOnBothSides() {
        for (int side : new int[] {1, -1}) {
            HandPose p = HandPose.VANILLA;
            float[] before = HandProjection.toScreen(HandProjection.anchor(p, side), W, H);
            HandPose moved = PoseGestures.move(p, side, 30, -20, H);
            float[] after = HandProjection.toScreen(HandProjection.anchor(moved, side), W, H);
            assertEquals(30, after[0] - before[0], 0.5);
            assertEquals(-20, after[1] - before[1], 0.5);
        }
    }

    @Test
    void anchorSitsInTheLowerCorner() {
        float[] right = HandProjection.toScreen(HandProjection.anchor(HandPose.VANILLA, 1), W, H);
        float[] left = HandProjection.toScreen(HandProjection.anchor(HandPose.VANILLA, -1), W, H);
        assertTrue(right[0] > W * 0.6 && right[1] > H * 0.6, "right " + right[0] + "," + right[1]);
        assertEquals(W - right[0], left[0], 1e-3);
    }

    @Test
    void wheelScalesAndPushesAway() {
        HandPose up = PoseGestures.wheelScale(HandPose.VANILLA, 1);
        assertEquals(PoseGestures.SCALE_STEP, up.scale(), 1e-5);
        assertEquals(1, PoseGestures.wheelScale(up, -1).scale(), 1e-5);
        assertTrue(PoseGestures.wheelDepth(HandPose.VANILLA, 2).z() < 0);
        float near = HandProjection.radius(HandPose.VANILLA, 1, H);
        float far = HandProjection.radius(PoseGestures.wheelDepth(HandPose.VANILLA, 5), 1, H);
        assertTrue(far < near);
    }

    @Test
    void rotationMirrorsForTheLeftHand() {
        HandPose r = PoseGestures.turn(HandPose.VANILLA, 1, 10, 5);
        HandPose l = PoseGestures.turn(HandPose.VANILLA, -1, 10, 5);
        assertEquals(r.rotX(), l.rotX(), 1e-5);
        assertEquals(-r.rotY(), l.rotY(), 1e-5);
        assertEquals(-PoseGestures.roll(HandPose.VANILLA, 1, 10).rotZ(), PoseGestures.roll(HandPose.VANILLA, -1, 10).rotZ(), 1e-5);
    }

    @Test
    void picksTheHandUnderThePointer() {
        EditSession s = new EditSession(HandPose.VANILLA, HandPose.VANILLA);
        float[] right = HandProjection.toScreen(HandProjection.anchor(HandPose.VANILLA, 1), W, H);
        assertEquals(EditSession.MAIN, HandPick.nearest(s, 1, right[0], right[1], W, H));
        assertEquals(EditSession.OFF, HandPick.nearest(s, -1, right[0], right[1], W, H));
        assertTrue(HandPick.over(HandPose.VANILLA, 1, right[0] + 5, right[1], W, H));
        assertTrue(!HandPick.over(HandPose.VANILLA, 1, W / 2, 10, W, H));
    }
}
