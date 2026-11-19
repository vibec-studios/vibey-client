package dev.henny.cosmetico.sim;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.state.PetMotion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetFollowerTest {
    private final Bundle pet = Fixtures.parse(Fixtures.cosmetic("t:pet", "pet"));
    private final PetMotion pm = new PetMotion();

    @Test
    void firstStepPlacesThePetRightAndBehind() {
        PetFollower.step(pm, pet, new PetInput(10, 64, 10, 0), 0);
        assertTrue(pm.init);
        assertEquals(10 - PetGeometry.side(pet), pm.x, 1e-9);
        assertEquals(10 - 0.35, pm.z, 1e-9);
        assertEquals(pm.x, pm.prevX, 1e-9);
    }

    @Test
    void followsWithLagAndTeleportsWhenFar() {
        PetFollower.step(pm, pet, new PetInput(10, 64, 10, 0), 0);
        double startX = pm.x;
        PetFollower.step(pm, pet, new PetInput(11, 64, 10, 0), 1);
        assertTrue(pm.x > startX && pm.x < startX + 1);
        assertTrue(pm.move > 0f);
        PetFollower.step(pm, pet, new PetInput(100, 64, 10, 0), 2);
        assertEquals(100 - PetGeometry.side(pet), pm.x, 1e-9);
        assertEquals(0f, pm.move);
    }

    @Test
    void neverStandsInsideThePlayer() {
        PetFollower.step(pm, pet, new PetInput(10, 64, 10, 0), 0);
        pm.x = 10.01;
        pm.z = 10;
        PetFollower.step(pm, pet, new PetInput(10, 64, 10, 0), 1);
        double d = Math.hypot(pm.x - 10, pm.z - 10);
        assertTrue(d > 0.2, "" + d);
    }

    @Test
    void noPetResetsAndGeometryHasDefaults() {
        PetFollower.step(pm, pet, new PetInput(0, 0, 0, 0), 0);
        PetFollower.step(pm, null, new PetInput(0, 0, 0, 0), 1);
        assertFalse(pm.init);
        assertEquals(0.25, PetGeometry.halfWidth(null), 1e-9);
        assertEquals(0.95, PetGeometry.side(null), 1e-9);
    }
}
