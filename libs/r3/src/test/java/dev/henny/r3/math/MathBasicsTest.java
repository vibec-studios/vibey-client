package dev.henny.r3.math;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MathBasicsTest {
    @Test
    void facesFromYawLikeMinecraft() {
        assertEquals(Face.SOUTH, Face.horizontalFromYaw(0));
        assertEquals(Face.WEST, Face.horizontalFromYaw(90));
        assertEquals(Face.NORTH, Face.horizontalFromYaw(180));
        assertEquals(Face.EAST, Face.horizontalFromYaw(-90));
        assertEquals(Face.SOUTH, Face.horizontalFromYaw(359));
    }

    @Test
    void faceHelpers() {
        assertEquals(Face.EAST, Face.NORTH.clockwise());
        assertEquals(Face.NORTH, Face.WEST.clockwise());
        assertEquals(Face.UP, Face.UP.clockwise());
        assertEquals(Face.DOWN, Face.UP.opposite());
        assertEquals(Face.WEST, Face.byName("west"));
        assertNull(Face.byName("sideways"));
        assertTrue(Face.DOWN.vertical());
        assertEquals(2, Face.SOUTH.axis());
    }

    @Test
    void facingBuildsRightHandedBasis() {
        Quaternionf q = Axes.facing(0.3, 0.2, -1, new Quaternionf());
        Vector3d r = Axes.right(q, new Vector3d());
        Vector3d u = Axes.up(q, new Vector3d());
        Vector3d n = Axes.normal(q, new Vector3d());
        assertEquals(0, r.dot(u), 1e-5);
        assertEquals(1, new Vector3d(r).cross(u).dot(n), 1e-5);
        assertEquals(0, r.y, 1e-5, "right stays horizontal");
        assertTrue(u.y > 0, "up points up");
    }

    @Test
    void facingStraightDownDoesNotDegenerate() {
        Quaternionf q = Axes.facing(0, -1, 0, new Quaternionf());
        assertEquals(-1, Axes.normal(q, new Vector3d()).y, 1e-5);
        assertEquals(1, Axes.right(q, new Vector3d()).length(), 1e-5);
    }

    @Test
    void argbHelpers() {
        assertEquals(0x80FFFFFF, Argb.withAlpha(0xFFFFFFFF, 128 / 255f));
        assertEquals(0x7F102030, Argb.multiplyAlpha(0xFE102030, 0.5f));
        assertEquals(0xFF808080, Argb.lerp(0xFF000000, 0xFFFFFFFF, 0.5f));
        assertEquals(0xFFFF0000, Argb.hsv(0f, 1f, 1f, 255));
        assertEquals(0xFF00FF00, Argb.hsv(1f / 3f, 1f, 1f, 255));
        assertEquals(0xFFFFFFFF, Argb.scaleRgb(0xFF808080, 3f));
    }

    @Test
    void smoothDampReachesTargetWithoutOvershoot() {
        SmoothDamp spring = new SmoothDamp(0.5f, 170f);
        float v = 0f;
        float max = 0f;
        for (int i = 0; i < 240; i++) {
            v = spring.step(v, 40f, 1f / 60f);
            max = Math.max(max, v);
        }
        assertEquals(40f, v, 0.05f);
        assertTrue(max <= 40f + 1e-4f);
    }
}
