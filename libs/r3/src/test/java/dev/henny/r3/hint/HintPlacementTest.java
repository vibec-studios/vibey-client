package dev.henny.r3.hint;

import dev.henny.r3.object.Transform;
import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.ScreenProjection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HintPlacementTest {
    final CameraState camera = new CameraState().set(0, 0, 0, 0, 0);

    @Test
    void projectsTheViewCenterToZero() {
        double[] out = new double[2];
        assertTrue(ScreenProjection.project(camera, 70, 16 / 9.0, 0, 0, 5, out));
        assertEquals(0, out[0], 1e-9);
        assertEquals(0, out[1], 1e-9);
        assertTrue(ScreenProjection.project(camera, 90, 1, 0, -5, 5, out));
        assertEquals(1, out[1], 1e-9, "45 degrees down is the bottom edge at fov 90");
        assertFalse(ScreenProjection.project(camera, 70, 1, 0, 0, -5, out));
    }

    @Test
    void lineGoesBelowThePanelWhenItCoversTheCrosshair() {
        Transform t = new Transform().size(2, 1).position(0, 0, 3);
        t.orientation().rotationY((float) Math.PI);
        double bottom = new HintPlacement().bottomOf(t, camera, 70, 16 / 9.0);
        assertTrue(bottom > 0 && bottom < 1, "bottom edge below the center, on screen: " + bottom);
        int y = HintPlacement.lineY(180, 360, 9, bottom, 4, 40);
        assertTrue(y > 180 + 4, "moved below the panel: " + y);
        assertEquals(184, HintPlacement.lineY(180, 360, 9, Double.NaN, 4, 40));
    }

    @Test
    void hugePanelsKeepTheLineUnderTheCrosshair() {
        Transform t = new Transform().size(20, 12).position(0, 0, 3);
        double bottom = new HintPlacement().bottomOf(t, camera, 70, 16 / 9.0);
        assertEquals(184, HintPlacement.lineY(180, 360, 9, bottom, 4, 40));
    }
}
