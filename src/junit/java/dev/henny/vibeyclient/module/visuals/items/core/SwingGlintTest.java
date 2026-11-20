package dev.henny.vibeyclient.module.visuals.items.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingGlintTest {
    @Test
    void vanillaCurveMatchesMinecraft() {
        float f = 0.3f, root = (float) Math.sqrt(f);
        SwingShape s = SwingShape.of(SwingStyle.VANILLA, f, 1);
        assertEquals(-0.4f * (float) Math.sin(root * Math.PI), s.tx(), 1e-6);
        assertEquals(0.2f * (float) Math.sin(root * 2 * Math.PI), s.ty(), 1e-6);
        assertEquals(-80f * (float) Math.sin(root * Math.PI), s.pitch(), 1e-4);
        assertEquals(45f, SwingShape.of(SwingStyle.VANILLA, 0, 1).yawIn(), 1e-6);
        assertEquals(-45f, SwingShape.of(SwingStyle.VANILLA, 0, -1).yawIn(), 1e-6);
    }

    @Test
    void ownCurvesRestAtVanillaAndSwingLess() {
        for (SwingStyle st : SwingStyle.values()) {
            SwingShape rest = SwingShape.of(st, 0, 1);
            assertEquals(0, rest.tx(), 1e-6);
            assertEquals(0, rest.pitch(), 1e-6);
            assertEquals(-rest.yawOut(), rest.yawIn(), 1e-6);
        }
        float vanilla = Math.abs(SwingShape.of(SwingStyle.VANILLA, 0.5f, 1).pitch());
        assertTrue(Math.abs(SwingShape.of(SwingStyle.SMOOTH, 0.5f, 1).pitch()) < vanilla);
        assertTrue(Math.abs(SwingShape.of(SwingStyle.PUNCH, 0.5f, 1).tz()) > Math.abs(SwingShape.of(SwingStyle.VANILLA, 0.5f, 1).tz()));
    }

    @Test
    void swingDurationScales() {
        assertEquals(6, SwingTiming.duration(6, 100));
        assertEquals(3, SwingTiming.duration(6, 200));
        assertEquals(12, SwingTiming.duration(6, 50));
        assertEquals(2, SwingTiming.duration(6, 1000));
        assertEquals(1, SwingTiming.duration(1, 300));
    }

    @Test
    void glintModulator() {
        assertArrayEquals(new float[] {1, 1, 1, 1}, GlintTint.modulator(false, 0xFFFFFF, 1), 1e-6f);
        assertArrayEquals(new float[] {0, 0, 0, 1}, GlintTint.modulator(true, 0xFF0000, 0), 1e-6f);
        float[] red = GlintTint.modulator(true, 0xFF0000, 1);
        assertTrue(red[0] > 1 && red[1] == 0 && red[2] == 0);
        float[] blue = GlintTint.modulator(true, 0x0000FF, 1);
        assertEquals(1, blue[2], 1e-5);
        float[] white = GlintTint.modulator(true, 0xFFFFFF, 1);
        for (int c = 0; c < 3; c++) {
            float energy = white[c] * white[c] * GlintTint.TEXTURE_ENERGY[c];
            assertEquals(GlintTint.TEXTURE_ENERGY[2], energy, 1e-4);
        }
    }

    @Test
    void dropScaleKeepsTheItemOnTheGround() {
        float minY = -0.25f, vanillaY = 0.1f + 0.25f + 0.0625f;
        float lifted = DropScale.vanillaLift(vanillaY, minY, 2f);
        assertEquals(0.1f + 0.0625f, lifted + minY * 2f, 1e-6);
        assertEquals(vanillaY, DropScale.vanillaLift(vanillaY, minY, 1f), 1e-6);
        assertEquals(0.4f, DropScale.physicsLift(0.2f, 2f), 1e-6);
    }
}
