package dev.henny.r3.math;

import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.PointerRays;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointerRaysTest {
    @Test
    void crosshairFollowsMinecraftYawConvention() {
        Ray ray = new Ray();
        PointerRays.fromCrosshair(new CameraState().set(1, 2, 3, 0, 0), ray);
        assertEquals(1, ray.direction().z(), 1e-9);
        assertEquals(1, ray.origin().x(), 1e-9);
        PointerRays.fromCrosshair(new CameraState().set(0, 0, 0, 90, 0), ray);
        assertEquals(-1, ray.direction().x(), 1e-6);
        PointerRays.fromCrosshair(new CameraState().set(0, 0, 0, 0, 90), ray);
        assertEquals(-1, ray.direction().y(), 1e-6);
    }

    @Test
    void cameraBasisIsOrthonormal() {
        CameraState c = new CameraState().set(0, 0, 0, 37, -20);
        assertEquals(0, c.right().dot(c.look()), 1e-6);
        assertEquals(0, c.up().dot(c.look()), 1e-6);
        assertEquals(1, c.up().length(), 1e-6);
        assertEquals(-1, new CameraState().set(0, 0, 0, 0, 0).right().x(), 1e-6);
    }

    @Test
    void screenCenterUnprojectsToViewDirection() {
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(70), 16f / 9f, 0.05f, 1000f);
        Matrix4f view = new Matrix4f();
        Ray ray = PointerRays.fromScreen(960, 540, 1920, 1080, proj, view, new Vector3d(10, 64, 10), new Ray());
        assertEquals(0, ray.direction().x(), 1e-5);
        assertEquals(0, ray.direction().y(), 1e-5);
        assertEquals(-1, ray.direction().z(), 1e-5);
        assertEquals(10, ray.origin().x(), 1e-3);
    }

    @Test
    void screenEdgeMatchesHalfFieldOfView() {
        float fov = (float) Math.toRadians(70);
        float aspect = 16f / 9f;
        Matrix4f proj = new Matrix4f().perspective(fov, aspect, 0.05f, 1000f);
        Ray ray = PointerRays.fromScreen(1920, 540, 1920, 1080, proj, new Matrix4f(), new Vector3d(), new Ray());
        double expected = Math.atan(Math.tan(fov / 2) * aspect);
        assertEquals(expected, Math.atan2(ray.direction().x(), -ray.direction().z()), 1e-4);
        Ray top = PointerRays.fromScreen(960, 0, 1920, 1080, proj, new Matrix4f(), new Vector3d(), new Ray());
        assertEquals(fov / 2, Math.atan2(top.direction().y(), -top.direction().z()), 1e-4);
    }

    @Test
    void viewMatrixIsWorldToCamera() {
        Matrix4f proj = new Matrix4f().perspective(1.2f, 1f, 0.05f, 100f);
        Matrix4f view = new Matrix4f().rotationY((float) Math.toRadians(90));
        Ray ray = PointerRays.fromScreen(50, 50, 100, 100, proj, view, new Vector3d(), new Ray());
        assertEquals(1, ray.direction().x(), 1e-5);
    }

    @Test
    void screenPointRayIsOrthographic() {
        Ray ray = PointerRays.fromScreenPoint(12, 34, new Ray());
        assertEquals(12, ray.origin().x(), 1e-9);
        assertEquals(1, ray.direction().z(), 1e-9);
    }

    @Test
    void anglesRoundTrip() {
        Vector3d d = Angles.direction(123f, -33f, new Vector3d());
        assertEquals(0f, Angles.wrapDegrees(Angles.yawOf(d.x, d.z) - 123f), 1e-3);
        assertEquals(-33f, Angles.pitchOf(d.x, d.y, d.z), 1e-3);
        assertEquals(-170f, Angles.wrapDegrees(190f), 1e-4);
    }
}
