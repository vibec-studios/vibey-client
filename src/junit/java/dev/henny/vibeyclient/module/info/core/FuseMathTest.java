package dev.henny.vibeyclient.module.info.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuseMathTest {
    @Test
    void secondsFromTicks() {
        assertEquals(4f, FuseMath.seconds(80, 0f), 1e-4);
        assertEquals(2.475f, FuseMath.seconds(50, 0.5f), 1e-4);
        assertEquals(0f, FuseMath.seconds(0, 0.9f), 1e-4);
    }

    @Test
    void labelHasOneDecimalWithoutLocaleComma() {
        assertEquals("2.4", FuseMath.label(2.43f));
        assertEquals("0.0", FuseMath.label(-1f));
        assertEquals("4.0", FuseMath.label(4f));
    }

    @Test
    void colorGoesFromGreenToRed() {
        assertEquals(0xFF3FE0C5, FuseMath.color(4f));
        assertEquals(0xFFFFE14D, FuseMath.color(2f));
        assertEquals(0xFFFF5470, FuseMath.color(0f));
    }

    @Test
    void urgentUnderOneSecond() {
        assertTrue(FuseMath.urgent(0.9f));
        assertFalse(FuseMath.urgent(1.5f));
    }

    @Test
    void colorsLerpAndAlpha() {
        assertEquals(0xFF808080, Colors.lerp(0xFF000000, 0xFFFFFFFF, 0.5f));
        assertEquals(0x80FFFFFF, Colors.alpha(0xFFFFFFFF, 0.5f));
        assertEquals(0x00FFFFFF, Colors.alpha(0xFFFFFFFF, -2f));
    }
}
