package dev.henny.vibeyclient.module.visuals.particles.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleStyleTest {
    @Test
    void clampsEveryValue() {
        ParticleStyle s = new ParticleStyle(999, 1, 0xFF123456, 500, false);
        assertEquals(ParticleStyle.MAX_AMOUNT, s.amount());
        assertEquals(ParticleStyle.MIN_SIZE, s.size());
        assertEquals(0x123456, s.tint());
        assertEquals(100, s.strength());
        assertEquals(0, new ParticleStyle(-5, 900, 0, -1, false).amount());
        assertEquals(ParticleStyle.MAX_SIZE, new ParticleStyle(100, 900, 0, 0, false).size());
    }

    @Test
    void vanillaIgnoresAnUnusedTint() {
        assertTrue(ParticleStyle.VANILLA.vanilla());
        assertTrue(ParticleStyle.VANILLA.withTint(0xFF0000).vanilla());
        assertFalse(ParticleStyle.VANILLA.withTint(0xFF0000).withStrength(10).vanilla());
        assertFalse(ParticleStyle.VANILLA.withHidden(true).vanilla());
    }

    @Test
    void fairNeverReduces() {
        ParticleStyle s = new ParticleStyle(20, 50, 0x00FF00, 40, true);
        assertTrue(s.reduces());
        ParticleStyle f = s.fair();
        assertEquals(100, f.amount());
        assertEquals(100, f.size());
        assertFalse(f.hidden());
        assertEquals(40, f.strength());
        ParticleStyle more = new ParticleStyle(300, 200, 0, 0, false);
        assertSame(more, more.fair());
    }

    @Test
    void storeDropsVanillaAndCountsRevisions() {
        ParticleStyles styles = new ParticleStyles();
        int r = styles.revision();
        styles.set("minecraft:flame", ParticleStyle.VANILLA.withAmount(200));
        assertEquals(1, styles.changed());
        assertEquals(200, styles.get("minecraft:flame").amount());
        styles.set("minecraft:flame", ParticleStyle.VANILLA);
        assertEquals(0, styles.changed());
        assertSame(ParticleStyle.VANILLA, styles.get("minecraft:flame"));
        assertTrue(styles.revision() >= r + 2);
    }
}
