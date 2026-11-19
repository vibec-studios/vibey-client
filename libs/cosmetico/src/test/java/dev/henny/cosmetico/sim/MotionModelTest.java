package dev.henny.cosmetico.sim;

import dev.henny.cosmetico.state.Avatar;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotionModelTest {
    private final Avatar a = new Avatar(UUID.randomUUID(), false);

    private static MotionInput standing(double y) {
        return new MotionInput(0, false, false, 0, y, 0, true, false, false, false, false, 0);
    }

    @Test
    void standingStillLongStartsIdleLong() {
        for (int i = 0; i < MotionModel.IDLE_LONG_TICKS; i++) MotionModel.step(a, standing(0), i * 0.05);
        assertEquals(0f, a.motion.idleLongTarget);
        MotionModel.step(a, standing(0), 11);
        assertEquals(1f, a.motion.idleLongTarget);
        assertEquals(11.0, a.motion.stateStart[6], 1e-9);
        assertTrue(a.motion.idleLong > 0f && a.motion.idleLong < 0.1f);
    }

    @Test
    void longFallsGlide() {
        double y = 100;
        for (int i = 0; i < 16; i++) {
            y -= 0.5;
            MotionModel.step(a, new MotionInput(0, false, false, 0, y, 0, false, false, false, false, false, 0), i);
        }
        assertEquals(1f, a.motion.glideTarget);
        assertEquals(15, a.motion.fallTicks);
    }

    @Test
    void sprintingNeedsMovementAndElytraIsFlight() {
        MotionModel.step(a, new MotionInput(0.9f, false, false, 0, 0, 0, true, false, false, true, false, 0), 0);
        assertEquals(1f, a.motion.sprintTarget);
        assertEquals(0.25f, a.motion.walk, 1e-6f);
        MotionModel.step(a, new MotionInput(0.9f, false, true, 1, 0, 0, false, false, false, true, false, 0), 1);
        assertEquals(0f, a.motion.sprintTarget);
        assertEquals(1f, a.motion.flyTarget);
        assertEquals(1f, a.motion.glideTarget);
        assertEquals(0f, a.motion.walkTarget);
    }
}
