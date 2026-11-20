package dev.henny.vibeyclient.module.camera.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlurMathTest {
    @Test
    void offAndInvalidGiveNoHistory() {
        assertEquals(0f, BlurMath.historyWeight(0, 0.016));
        assertEquals(0f, BlurMath.historyWeight(5, 0));
        assertEquals(0f, BlurMath.historyWeight(5, Double.NaN));
    }

    @Test
    void strongerMeansLongerTrail() {
        float prev = 0;
        for (int s = 1; s <= BlurMath.MAX_STRENGTH; s++) {
            float w = BlurMath.historyWeight(s, 1 / 60.0);
            assertTrue(w > prev, "strength " + s);
            prev = w;
        }
        assertEquals(BlurMath.historyWeight(10, 1 / 60.0), BlurMath.historyWeight(99, 1 / 60.0));
    }

    @Test
    void sameTrailAtAnyFrameRate() {
        double at60 = BlurMath.historyWeight(4, 1 / 60.0);
        double at240 = Math.pow(BlurMath.historyWeight(4, 1 / 240.0), 4);
        assertEquals(at60, at240, 1e-4);
    }

    @Test
    void weightIsCapped() {
        assertEquals(BlurMath.MAX_WEIGHT, BlurMath.historyWeight(10, 1e-5), 1e-6);
    }

    @Test
    void teleportResetsHistory() {
        assertFalse(BlurMath.jumped(1, 1, 1));
        assertTrue(BlurMath.jumped(5, 0, 0));
        assertEquals(0.048f, BlurMath.trailSeconds(4), 1e-6);
    }
}
