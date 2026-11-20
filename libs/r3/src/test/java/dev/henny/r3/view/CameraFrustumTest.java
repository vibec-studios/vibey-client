package dev.henny.r3.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraFrustumTest {
    final CameraState camera = new CameraState().set(0, 64, 0, 0, 0);
    final CameraFrustum frustum = new CameraFrustum().set(camera, 70, 16 / 9.0, 0);

    static boolean box(CameraFrustum f, double x, double y, double z, double r) {
        return f.isVisible(x - r, y - r, z - r, x + r, y + r, z + r);
    }

    @Test
    void inFrontIsVisibleBehindIsNot() {
        assertTrue(box(frustum, 0, 64, 10, 0.5));
        assertFalse(box(frustum, 0, 64, -10, 0.5));
    }

    @Test
    void farToTheSideOrAboveIsCulled() {
        assertFalse(box(frustum, 30, 64, 5, 0.5));
        assertFalse(box(frustum, 0, 90, 5, 0.5));
        assertTrue(box(frustum, 6, 64, 10, 0.5));
    }

    @Test
    void boxesAroundTheCameraOrOverlappingAnEdgeStayVisible() {
        assertTrue(box(frustum, 0, 64, -1, 2));
        assertTrue(box(frustum, 14, 64, 10, 6));
    }

    @Test
    void followsTheCameraRotation() {
        camera.rotation(90, 0);
        CameraFrustum f = new CameraFrustum().set(camera, 70, 16 / 9.0, 0);
        assertTrue(box(f, -10, 64, 0, 0.5));
        assertFalse(box(f, 0, 64, 10, 0.5));
    }
}
