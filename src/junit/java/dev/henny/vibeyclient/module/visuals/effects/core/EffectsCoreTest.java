package dev.henny.vibeyclient.module.visuals.effects.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectsCoreTest {
    @Test
    void shakeDecaysAndStaysSmall() {
        Shake s = new Shake();
        assertEquals(0, s.roll(1), 0);
        s.kick(1);
        float peak = 0;
        for (int i = 0; i < 20; i++) {
            s.step(0.02f);
            peak = Math.max(peak, Math.abs(s.roll(1)));
        }
        assertTrue(peak > 0.5f && peak <= Shake.ROLL_DEG);
        for (int i = 0; i < 200; i++) s.step(0.05f);
        assertEquals(0, s.energy(), 0);
        s.kick(1);
        s.step(0.02f);
        assertEquals(0, s.roll(0), 0);
    }

    @Test
    void shakePowerFallsWithDistance() {
        assertEquals(1, Shake.power(0, 4), 1e-6);
        assertEquals(0.5, Shake.power(8, 4), 1e-6);
        assertEquals(0, Shake.power(40, 4), 0);
        assertEquals(0.5, Shake.power(4, 0.5f), 1e-6);
    }

    @Test
    void kickKeepsTheStrongest() {
        Shake s = new Shake();
        s.kick(0.8f);
        s.kick(0.3f);
        assertEquals(0.8f, s.energy(), 1e-6);
        s.kick(3f);
        assertEquals(1f, s.energy(), 1e-6);
    }

    @Test
    void fairnessClampsReductions() {
        assertEquals(0.5f, EffectMath.fair(50, false), 1e-6);
        assertEquals(1f, EffectMath.fair(50, true), 1e-6);
        assertEquals(1.5f, EffectMath.fair(150, true), 1e-6);
        assertEquals(EffectMath.FIRE_ALPHA * 0.3f, EffectMath.fireAlpha(30, false), 1e-6);
        assertEquals(EffectMath.FIRE_ALPHA, EffectMath.fireAlpha(30, true), 1e-6);
        assertEquals(EffectMath.FIRE_ALPHA, EffectMath.fireAlpha(150, false), 1e-6);
        assertEquals(0.25f, EffectMath.overlayAlpha(1f, 25, false), 1e-6);
    }

    @Test
    void previewFadesInAndOut() {
        assertEquals(0, PreviewCurve.level(0, 3000, 100), 0);
        assertEquals(0.5f, PreviewCurve.level(1000, 3000, 1200), 1e-6);
        assertEquals(1f, PreviewCurve.level(1000, 3000, 2500), 1e-6);
        assertEquals(0, PreviewCurve.level(1000, 3000, 4000), 0);
        assertTrue(PreviewCurve.active(1000, 3000, 3999));
    }

    @Test
    void presetsCoverEveryKey() {
        for (Preset p : Preset.values()) {
            Map<String, Integer> v = EffectPresets.values(p);
            for (String k : EffectKeys.PERCENTS) assertTrue(v.containsKey(k), p + " " + k);
            assertTrue(v.containsKey(EffectKeys.OWN_POTION));
            assertEquals(p, Preset.of(p.id()));
        }
        Map<String, Integer> vanilla = EffectPresets.values(Preset.VANILLA);
        assertEquals(0, vanilla.get(EffectKeys.BLAST));
        assertEquals(100, vanilla.get(EffectKeys.FIRE));
        assertEquals(60, EffectPresets.values(Preset.PVP).get(EffectKeys.FIRE));
    }
}
