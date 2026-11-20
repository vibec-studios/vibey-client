package dev.henny.vibeyclient.ui.hud.edit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScaleStepsTest {
    @Test
    void stepsUpAndDown() {
        assertEquals(1.1f, ScaleSteps.next(1f, 1));
        assertEquals(0.9f, ScaleSteps.next(1f, -1));
        assertEquals(1.25f, ScaleSteps.next(1.13f, 1));
        assertEquals(1.1f, ScaleSteps.next(1.13f, -1));
        assertEquals(4f, ScaleSteps.next(4f, 1));
        assertEquals(0.5f, ScaleSteps.next(0.5f, -1));
    }

    @Test
    void percent() {
        assertEquals(125, ScaleSteps.percent(1.25f));
        assertEquals(60, ScaleSteps.percent(0.6f));
    }
}
