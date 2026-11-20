package dev.henny.vibeyclient.module.visuals.particles.core;

import dev.henny.vibeyclient.module.visuals.effects.core.Preset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticlePresetsTest {
    private static final List<String> IDS = List.of("minecraft:crit", "minecraft:enchanted_hit", "minecraft:rain",
            "minecraft:ash", "minecraft:block", "minecraft:entity_effect", "minecraft:heart", "minecraft:flame",
            "minecraft:totem_of_undying", "somemod:spark");

    @Test
    void vanillaIsEmpty() {
        assertTrue(ParticlePresets.styles(Preset.VANILLA, IDS).isEmpty());
        assertTrue(ParticlePresets.matches(Preset.VANILLA, new ParticleStyles(), IDS));
    }

    @Test
    void pvpMakesHitsBiggerAndNoiseLower() {
        Map<String, ParticleStyle> m = ParticlePresets.styles(Preset.PVP, IDS);
        assertEquals(125, m.get("minecraft:crit").size());
        assertTrue(m.get("minecraft:rain").amount() < 100);
        assertTrue(m.get("minecraft:block").amount() < 100);
        assertFalse(m.containsKey("somemod:spark"));
        assertFalse(m.values().stream().anyMatch(ParticleStyle::hidden));
    }

    @Test
    void fancyOnlyAdds() {
        Map<String, ParticleStyle> m = ParticlePresets.styles(Preset.FANCY, IDS);
        assertFalse(m.isEmpty());
        assertFalse(m.values().stream().anyMatch(ParticleStyle::reduces));
        assertEquals(200, m.get("minecraft:totem_of_undying").amount());
    }

    @Test
    void matchesAfterApplying() {
        ParticleStyles s = new ParticleStyles();
        s.replaceAll(ParticlePresets.styles(Preset.CLEAN, IDS));
        assertTrue(ParticlePresets.matches(Preset.CLEAN, s, IDS));
        assertFalse(ParticlePresets.matches(Preset.PVP, s, IDS));
        s.set("minecraft:heart", ParticleStyle.VANILLA.withSize(200));
        assertFalse(ParticlePresets.matches(Preset.CLEAN, s, IDS));
    }
}
