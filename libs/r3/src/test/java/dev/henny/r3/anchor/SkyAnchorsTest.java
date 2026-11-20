package dev.henny.r3.anchor;

import dev.henny.r3.math.Angles;
import dev.henny.r3.object.Transform;
import dev.henny.r3.persist.AnchorCodec;
import dev.henny.r3.support.FakeWorld;
import dev.henny.r3.view.CameraState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyAnchorsTest {
    private static final float DT = 1f / 60f;
    private final CameraState camera = new CameraState().set(0, 65, 0, 0, 0);
    private final AnchorContext ctx = new AnchorContext().set(new FakeWorld(), camera, DT);

    private float yawOf(Transform t) {
        return Angles.yawOf(t.position().x - camera.position().x(), t.position().z - camera.position().z());
    }

    private float elevationOf(Transform t) {
        return -Angles.pitchOf(t.position().x - camera.position().x(), t.position().y - camera.position().y(),
                t.position().z - camera.position().z());
    }

    @Test
    void softFollowRestsForSmallTurnsAndRecentersForBigOnes() {
        SoftFollowAnchor a = SoftFollowAnchor.soft(48, 20);
        Transform t = new Transform();
        a.resolve(ctx, t);
        assertEquals(20, elevationOf(t), 0.01);
        camera.rotation(25, 0);
        for (int i = 0; i < 60; i++) a.resolve(ctx, t);
        assertEquals(0, yawOf(t), 0.01, "25 degrees is inside the 30 degree zone");
        camera.rotation(60, 0);
        a.resolve(ctx, t);
        assertTrue(a.recentering());
        for (int i = 0; i < 240; i++) a.resolve(ctx, t);
        assertFalse(a.recentering());
        assertEquals(60, yawOf(t), SoftFollowAnchor.SETTLE_DEGREES + 0.1);
    }

    @Test
    void followGlidesAfterEveryTurn() {
        SoftFollowAnchor a = SoftFollowAnchor.follow(40, 15);
        Transform t = new Transform();
        a.resolve(ctx, t);
        camera.rotation(10, 0);
        a.resolve(ctx, t);
        float first = yawOf(t);
        assertTrue(first > 0 && first < 10, "eases after the view, was " + first);
    }

    @Test
    void fixedNeverTurns() {
        SoftFollowAnchor a = SoftFollowAnchor.fixed(40, 90, 15);
        Transform t = new Transform();
        a.resolve(ctx, t);
        camera.rotation(-170, 0);
        for (int i = 0; i < 120; i++) a.resolve(ctx, t);
        assertEquals(90, yawOf(t), 0.01);
    }

    @Test
    void lookingDownLowersTheTextButNotBelowTwoDegrees() {
        LowersWhenLookingDown a = new LowersWhenLookingDown(SoftFollowAnchor.follow(40, 20));
        Transform t = new Transform();
        camera.rotation(0, 12);
        a.resolve(ctx, t);
        assertEquals(8, elevationOf(t), 0.05);
        camera.rotation(0, 60);
        for (int i = 0; i < 300; i++) a.resolve(ctx, t);
        assertEquals(LowersWhenLookingDown.FLOOR_DEGREES, elevationOf(t), 0.05);
        camera.rotation(0, -30);
        for (int i = 0; i < 300; i++) a.resolve(ctx, t);
        assertEquals(20, elevationOf(t), 0.05, "looking up keeps the set height");
        assertEquals(1f, LowersWhenLookingDown.lowered(1f, 30f), 1e-6, "low settings stay as they are");
    }

    @Test
    void codecKeepsBothAnchors() {
        Anchor back = AnchorCodec.decode(AnchorCodec.encode(new LowersWhenLookingDown(SoftFollowAnchor.soft(48, 18))));
        LowersWhenLookingDown l = assertInstanceOf(LowersWhenLookingDown.class, back);
        SoftFollowAnchor s = assertInstanceOf(SoftFollowAnchor.class, l.inner());
        assertEquals(48, s.distance(), 1e-9);
        assertEquals(18, s.elevation(), 1e-6);
        assertEquals(SoftFollowAnchor.SOFT_RECENTER_DEGREES, s.recenterDegrees(), 1e-6);
        assertEquals(SoftFollowAnchor.SOFT_RATE, s.rate(), 1e-6);
    }
}
