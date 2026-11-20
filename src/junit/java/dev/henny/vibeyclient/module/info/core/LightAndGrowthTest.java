package dev.henny.vibeyclient.module.info.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightAndGrowthTest {
    @Test
    void overworldSpawnLight() {
        assertEquals(SpawnLight.SAFE, SpawnLight.classify(1, 0, 0, 7));
        assertEquals(SpawnLight.ALWAYS, SpawnLight.classify(0, 0, 0, 7));
        assertEquals(SpawnLight.ALWAYS, SpawnLight.classify(0, 7, 0, 7));
        assertEquals(SpawnLight.NIGHT, SpawnLight.classify(0, 15, 0, 7));
    }

    @Test
    void netherLikeLimits() {
        assertEquals(SpawnLight.ALWAYS, SpawnLight.classify(7, 0, 15, 7));
        assertEquals(SpawnLight.SAFE, SpawnLight.classify(8, 0, 15, 7));
        assertEquals(SpawnLight.ALWAYS, SpawnLight.classify(11, 0, 15, 11));
    }

    @Test
    void growthPercent() {
        assertEquals(0, Growth.percent(0, 7));
        assertEquals(57, Growth.percent(4, 7));
        assertEquals(100, Growth.percent(9, 7));
        assertEquals(100, Growth.percent(0, 0));
        assertEquals("43%", Growth.label(3, 7));
        assertTrue(Growth.mature(7, 7));
        assertFalse(Growth.mature(6, 7));
    }

    @Test
    void healthOnlyWhereVanillaShowsIt() {
        assertTrue(HealthRule.visible(true, false));
        assertTrue(HealthRule.visible(false, true));
        assertFalse(HealthRule.visible(false, false));
        assertEquals("13/20", HealthRule.label(12.5f, 20f));
        assertEquals("0/30", HealthRule.label(-1f, 30f));
    }

    @Test
    void modLabels() {
        assertEquals("Minecraft", ModLabel.of("minecraft", ns -> null));
        assertEquals("Create", ModLabel.of("create", ns -> "Create"));
        assertEquals("Farmers delight", ModLabel.of("farmers_delight", ns -> null));
        assertEquals("", ModLabel.of("", ns -> "x"));
    }
}
