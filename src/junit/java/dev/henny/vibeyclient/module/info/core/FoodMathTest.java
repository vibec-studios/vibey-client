package dev.henny.vibeyclient.module.info.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodMathTest {
    @Test
    void steakOnHalfHunger() {
        FoodGain g = FoodMath.eat(10, 0f, 8, 12.8f);
        assertEquals(18, g.food());
        assertEquals(8, g.foodAdded());
        assertEquals(12.8f, g.saturation(), 1e-4);
        assertFalse(g.wasted());
    }

    @Test
    void saturationNeverAboveFood() {
        FoodGain g = FoodMath.eat(2, 0f, 2, 9.6f);
        assertEquals(4, g.food());
        assertEquals(4f, g.saturation(), 1e-4);
        assertEquals(4f, g.saturationAdded(), 1e-4);
    }

    @Test
    void fullBarWastesNutrition() {
        FoodGain g = FoodMath.eat(18, 5f, 6, 7.2f);
        assertEquals(20, g.food());
        assertEquals(2, g.foodAdded());
        assertTrue(g.wasted());
        assertEquals(12.2f, g.saturation(), 1e-4);
    }

    @Test
    void clampsBrokenInput() {
        FoodGain g = FoodMath.eat(25, 30f, 4, 2f);
        assertEquals(20, g.food());
        assertEquals(0, g.foodAdded());
        assertEquals(20f, g.saturation(), 1e-4);
    }

    @Test
    void saturationFromModifier() {
        assertEquals(12.8f, FoodMath.saturationOf(8, 0.8f), 1e-4);
        assertEquals(0.4f, FoodMath.saturationOf(2, 0.1f), 1e-4);
    }

    @Test
    void tableKnowsCakeAndRiskyFood() {
        FoodValue cake = FoodTable.withoutComponent("minecraft:cake");
        assertNotNull(cake);
        assertEquals(2, cake.nutrition());
        assertNull(FoodTable.withoutComponent("minecraft:bread"));
        assertTrue(FoodTable.risky("minecraft:rotten_flesh"));
        assertFalse(FoodTable.risky("minecraft:cooked_beef"));
    }
}
