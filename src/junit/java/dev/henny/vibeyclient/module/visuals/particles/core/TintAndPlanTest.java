package dev.henny.vibeyclient.module.visuals.particles.core;

import dev.henny.vibeyclient.module.visuals.core.ParticleBudget;
import dev.henny.vibeyclient.module.visuals.core.ParticleCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TintAndPlanTest {
    @Test
    void tintTurnsWhiteIntoTheColour() {
        assertEquals(0xFFFF0000, TintMath.apply(0xFFFFFFFF, 0xFF0000, 100));
        assertEquals(0x80FFFFFF, TintMath.apply(0x80FFFFFF, 0xFF0000, 0));
        int half = TintMath.apply(0xFFFFFFFF, 0x0000FF, 50);
        assertEquals(0x80, (half >> 16) & 255);
        assertEquals(0xFF, half & 255);
        assertEquals(0xFF590000, TintMath.apply(0xFF000000, 0xFF0000, 100));
        assertEquals(0xFFBDBD00, TintMath.apply(0xFF999999, 0xFFFF00, 100));
    }

    @Test
    void hexRoundTrip() {
        assertEquals("#3fe0c5", TintMath.hex(0xFF3FE0C5));
        assertEquals(0x3FE0C5, TintMath.parseHex("#3FE0C5", 0));
        assertEquals(0x3FE0C5, TintMath.parseHex("ff3fe0c5", 0));
        assertEquals(7, TintMath.parseHex("nope", 7));
        assertEquals(7, TintMath.parseHex(null, 7));
    }

    @Test
    void amountMultipliesWithTheGlobalSlider() {
        assertEquals(200, sum(ParticleCategory.AMBIENT, 100, false, ParticleStyle.VANILLA.withAmount(200)));
        assertEquals(50, sum(ParticleCategory.AMBIENT, 50, false, ParticleStyle.VANILLA));
        assertEquals(300, sum(ParticleCategory.OTHER, 200, false, ParticleStyle.VANILLA.withAmount(150)));
        assertEquals(600, sum(ParticleCategory.OTHER, 300, false, ParticleStyle.VANILLA.withAmount(400)));
    }

    @Test
    void hiddenAndExplicitZeroWinOverKeepImportant() {
        assertEquals(100, sum(ParticleCategory.IMPORTANT, 0, true, ParticleStyle.VANILLA));
        assertEquals(0, sum(ParticleCategory.IMPORTANT, 0, true, ParticleStyle.VANILLA.withAmount(0)));
        assertEquals(0, sum(ParticleCategory.IMPORTANT, 100, true, ParticleStyle.VANILLA.withHidden(true)));
        assertEquals(0, sum(ParticleCategory.WEATHER, 100, false, ParticleStyle.VANILLA.withHidden(true)));
    }

    @Test
    void ambientOffStillWorks() {
        ParticleBudget b = new ParticleBudget();
        assertEquals(0, StylePlan.count(ParticleCategory.WEATHER, 100, true, true, ParticleStyle.VANILLA.withAmount(300), b));
        assertTrue(StylePlan.neutral(100, false, ParticleStyle.VANILLA.withSize(150)));
        assertFalse(StylePlan.neutral(100, false, ParticleStyle.VANILLA.withAmount(150)));
        assertFalse(StylePlan.neutral(100, true, ParticleStyle.VANILLA));
    }

    @Test
    void prettyNames() {
        assertEquals("Soul fire flame", ParticleNames.pretty("minecraft:soul_fire_flame"));
        assertEquals("Spark (somemod)", ParticleNames.pretty("somemod:spark"));
        assertTrue(ParticleNames.matches("minecraft:soul_fire_flame", "Soul fire flame", "fire fl"));
        assertTrue(ParticleNames.matches("minecraft:soul_fire_flame", "Soul fire flame", ""));
        assertFalse(ParticleNames.matches("minecraft:crit", "Crit", "rain"));
    }

    private static int sum(ParticleCategory c, int global, boolean keep, ParticleStyle s) {
        ParticleBudget b = new ParticleBudget();
        int n = 0;
        for (int i = 0; i < 100; i++) n += StylePlan.count(c, global, keep, false, s, b);
        return n;
    }
}
