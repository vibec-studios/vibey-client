package dev.henny.vibeyclient.module.cinematic.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClearanceTest {
    private final GridWorld w = GridWorld.flat().fill(5, 65, -3, 5, 70, 3, GridWorld.STONE);

    @Test
    void freeOnlyInAirWithMargin() {
        assertTrue(Clearance.free(w, new V3(0.5, 66.5, 0.5)));
        assertFalse(Clearance.free(w, new V3(5.5, 66.5, 0.5)), "inside the wall");
        assertFalse(Clearance.free(w, new V3(4.85, 66.5, 0.5)), "too close to the wall");
        assertFalse(Clearance.free(w, new V3(0.5, 65.1, 0.5)), "touching the floor");
        assertFalse(Clearance.free(w, new V3(500, 80, 0)), "not loaded");
    }

    @Test
    void segmentsNeverPassThroughBlocks() {
        assertTrue(Clearance.segment(w, new V3(0.5, 67, -5), new V3(0.5, 67, 5)));
        assertFalse(Clearance.segment(w, new V3(0.5, 67, 0.5), new V3(9.5, 67, 0.5)));
    }

    @Test
    void sightIsBlockedByOpaqueBlocksOnly() {
        assertTrue(Clearance.sight(w, new V3(0.5, 66.6, 0.5), new V3(-6, 72, 4)));
        assertFalse(Clearance.sight(w, new V3(0.5, 66.6, 0.5), new V3(9.5, 67, 0.5)));
        GridWorld glass = GridWorld.flat().fill(5, 65, -3, 5, 70, 3, GridWorld.GLASS);
        assertTrue(Clearance.sight(glass, new V3(0.5, 66.6, 0.5), new V3(9.5, 67, 0.5)), "glass is see-through");
        assertTrue(Clearance.sight(w, new V3(0.5, 66.6, 0.5), new V3(0.5, 66.6, 0.5)));
        assertTrue(Clearance.sight(GridWorld.flat(), new V3(4.775, 66.72, 3.965), new V3(6, 66.62, 0)), "diagonal");
    }

    @Test
    void openSkyNeedsNothingAbove() {
        assertTrue(Clearance.openSky(w, new V3(0.5, 66, 0.5), 48));
        GridWorld roof = GridWorld.flat().set(0, 80, 0, GridWorld.PLANKS);
        assertFalse(Clearance.openSky(roof, new V3(0.5, 66, 0.5), 48));
    }
}
