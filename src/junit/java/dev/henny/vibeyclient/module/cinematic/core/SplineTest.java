package dev.henny.vibeyclient.module.cinematic.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplineTest {
    @Test
    void passesThroughEndsAndMiddle() {
        V3 a = new V3(0, 0, 0), b = new V3(5, 2, 0), c = new V3(10, 0, 3);
        Spline s = Spline.of(a, b, c);
        assertTrue(s.at(0).dist(a) < 1e-6);
        assertTrue(s.at(1).dist(c) < 1e-6);
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= 200; i++) best = Math.min(best, s.at(i / 200.0).dist(b));
        assertTrue(best < 0.05, "middle point on the curve, off by " + best);
    }

    @Test
    void evenSpeedAlongTheCurve() {
        Spline s = Spline.of(new V3(0, 0, 0), new V3(1, 0, 0), new V3(9, 0, 4), new V3(10, 3, 12));
        double step = s.length() / 50;
        for (int i = 0; i < 50; i++) {
            double d = s.at(i / 50.0).dist(s.at((i + 1) / 50.0));
            assertEquals(step, d, step * 0.12, "segment " + i);
        }
    }

    @Test
    void singleAndDoublePoints() {
        V3 p = new V3(3, 4, 5);
        assertEquals(p, Spline.of(p).at(0.7));
        assertEquals(0, Spline.of(p).length(), 1e-9);
        Spline line = Spline.of(new V3(0, 0, 0), new V3(10, 0, 0));
        assertEquals(5, line.at(0.5).x(), 1e-6);
        assertEquals(10, line.length(), 1e-6);
    }

    @Test
    void easingStaysInRangeAndIsMonotonic() {
        double prev = -1;
        for (int i = 0; i <= 100; i++) {
            double v = Ease.gentle(i / 100.0);
            assertTrue(v >= prev - 1e-12 && v >= 0 && v <= 1);
            prev = v;
        }
        assertEquals(0, Ease.smoother(-1));
        assertEquals(1, Ease.smoother(2));
        assertEquals(0.5, Ease.inOutSine(0.5), 1e-9);
    }

    @Test
    void lookMatchesDirection() {
        V3 from = new V3(0, 64, 0), to = new V3(-3, 66, 7);
        V3 d = Look.direction(Look.yaw(from, to), Look.pitch(from, to));
        assertTrue(d.dist(to.sub(from).norm()) < 1e-5);
        assertEquals(-170, Look.wrap(Look.lerpAngle(170, -170, 1)), 1e-4);
        assertEquals(-180, Look.wrap(Look.lerpAngle(170, -170, 0.5)), 1e-4);
    }
}
