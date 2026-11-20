package dev.henny.vibeyclient.module.visuals.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticlesTest {
    @Test
    void tableKnowsVanillaIds() {
        assertEquals(ParticleCategory.IMPORTANT, ParticleTable.category("minecraft:crit"));
        assertEquals(ParticleCategory.IMPORTANT, ParticleTable.category("damage_indicator"));
        assertEquals(ParticleCategory.WEATHER, ParticleTable.category("minecraft:rain"));
        assertEquals(ParticleCategory.AMBIENT, ParticleTable.category("minecraft:ash"));
        assertEquals(ParticleCategory.BLOCK, ParticleTable.category("minecraft:block"));
        assertEquals(ParticleCategory.EFFECT, ParticleTable.category("minecraft:entity_effect"));
        assertEquals(ParticleCategory.OTHER, ParticleTable.category("minecraft:heart"));
        assertEquals(ParticleCategory.OTHER, ParticleTable.category("somemod:crit"));
        assertEquals(ParticleCategory.OTHER, ParticleTable.category(null));
        assertTrue(ParticleTable.size() > 80);
    }

    @Test
    void budgetCarriesFractions() {
        ParticleBudget b = new ParticleBudget();
        int sum = 0;
        for (int i = 0; i < 100; i++) sum += b.next(0.5);
        assertEquals(50, sum);
        sum = 0;
        for (int i = 0; i < 100; i++) sum += b.next(2.5);
        assertEquals(250, sum);
        assertEquals(0, b.next(0));
        assertEquals(0.0, b.carry());
    }

    @Test
    void planKeepsImportantParticles() {
        ParticlePlan p = new ParticlePlan();
        assertEquals(0, p.count(ParticleCategory.OTHER, 0, true, false));
        assertEquals(1, p.count(ParticleCategory.IMPORTANT, 0, true, false));
        assertEquals(0, p.count(ParticleCategory.IMPORTANT, 0, false, false));
        assertEquals(2, p.count(ParticleCategory.IMPORTANT, 200, true, false));
        assertEquals(1, p.count(ParticleCategory.OTHER, 100, true, false));
        assertEquals(2, p.count(ParticleCategory.BLOCK, 200, true, false));
        assertEquals(3, p.count(ParticleCategory.BLOCK, 999, true, false));
    }

    @Test
    void ambientOffHidesAmbientAndWeatherOnly() {
        ParticlePlan p = new ParticlePlan();
        assertEquals(0, p.count(ParticleCategory.AMBIENT, 100, true, true));
        assertEquals(0, p.count(ParticleCategory.WEATHER, 300, true, true));
        assertEquals(1, p.count(ParticleCategory.BLOCK, 100, true, true));
        assertEquals(1, p.count(ParticleCategory.IMPORTANT, 100, true, true));
    }
}
