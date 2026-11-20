package dev.henny.r3.anchor;

import dev.henny.r3.object.Transform;
import dev.henny.r3.support.FakeWorld;
import dev.henny.r3.view.CameraState;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorsTest {
    private final FakeWorld world = new FakeWorld();
    private final CameraState camera = new CameraState().set(0, 65, 0, 0, 0);
    private final AnchorContext ctx = new AnchorContext().set(world, camera, 1f / 60f);

    @Test
    void entityAnchorFloatsAboveTheHead() {
        UUID id = UUID.randomUUID();
        world.entity(id, 3, 64, 4, 1.8f, 0, false);
        Transform t = new Transform();
        assertTrue(EntityAnchor.aboveHead(id, 0.5).resolve(ctx, t));
        assertEquals(66.3, t.position().y, 1e-6);
        assertEquals(3, t.position().x, 1e-6);
        Vector3d n = t.normal(new Vector3d());
        assertTrue(n.x < 0 && n.z < 0, "faces the camera");
        assertEquals(0, n.y, 1e-6, "upright");
    }

    @Test
    void sneakingLowersTheLabel() {
        UUID id = UUID.randomUUID();
        world.entity(id, 0, 64, 4, 1.8f, 0, true);
        Transform t = new Transform();
        EntityAnchor.aboveHead(id, 0.5).resolve(ctx, t);
        assertEquals(66.3 - EntityAnchor.SNEAK_DROP, t.position().y, 1e-5);
    }

    @Test
    void missingEntityIsNotResolved() {
        assertFalse(EntityAnchor.aboveHead(UUID.randomUUID(), 0.5).resolve(ctx, new Transform()));
    }

    @Test
    void cameraAnchorGluedInFrontOfView() {
        Transform t = new Transform();
        CameraAnchor.glued(2).resolve(ctx, t);
        assertEquals(2, t.position().z, 1e-6);
        assertEquals(-1, t.normal(new Vector3d()).z, 1e-5);
    }

    @Test
    void smoothedCameraAnchorLagsBehindATurn() {
        CameraAnchor a = new CameraAnchor(2, 0, 0, 4.5f);
        Transform t = new Transform();
        a.resolve(ctx, t);
        camera.rotation(90, 0);
        a.resolve(ctx, t);
        assertTrue(t.position().z > 0.5, "still mostly where it was");
        for (int i = 0; i < 300; i++) a.resolve(ctx, t);
        assertEquals(-2, t.position().x, 1e-3);
    }

    @Test
    void viewFollowRestsInsideTheComfortZone() {
        ViewFollowAnchor a = new ViewFollowAnchor(48);
        a.step(0, 0, 0.016f);
        a.step(15, 0, 0.016f);
        for (int i = 0; i < 60; i++) a.step(15, 0, 0.016f);
        assertEquals(0, a.yaw(), 1e-4);
        assertFalse(a.gliding());
    }

    @Test
    void viewFollowGlidesAfterTheDelay() {
        ViewFollowAnchor a = new ViewFollowAnchor(48);
        a.step(0, 0, 0.016f);
        a.step(40, 0, 0.1f);
        assertFalse(a.gliding(), "waits a moment first");
        for (int i = 0; i < 5; i++) a.step(40, 0, 0.1f);
        assertTrue(a.gliding() || Math.abs(a.yaw() - 40) < 1.5f);
        for (int i = 0; i < 200; i++) a.step(40, 0, 0.016f);
        assertEquals(40, a.yaw(), 1.5f);
    }

    @Test
    void viewFollowJumpsAtOnceWhenFarAway() {
        ViewFollowAnchor a = new ViewFollowAnchor(48);
        a.step(0, 0, 0.016f);
        a.step(120, 0, 0.016f);
        assertTrue(a.gliding());
    }

    @Test
    void viewFollowStaysBetweenElevationLimits() {
        ViewFollowAnchor a = new ViewFollowAnchor(48);
        a.step(0, 80, 0.016f);
        assertEquals(ViewFollowAnchor.MIN_ELEVATION, a.elevation(), 1e-4);
        a.reset();
        a.step(0, -80, 0.016f);
        assertEquals(ViewFollowAnchor.MAX_ELEVATION, a.elevation(), 1e-4);
    }

    @Test
    void entityYawFacingUsesBodyRotation() {
        UUID id = UUID.randomUUID();
        world.entity(id, 0, 64, 0, 1.8f, 90, false);
        Transform t = new Transform();
        new EntityAnchor(id, new Vector3d(), false, Facing.ENTITY_YAW).resolve(ctx, t);
        assertEquals(-1, t.normal(new Vector3d()).x, 1e-5);
    }
}
