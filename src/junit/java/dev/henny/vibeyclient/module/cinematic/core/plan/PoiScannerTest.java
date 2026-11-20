package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.GridWorld;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiScannerTest {
    @Test
    void buildsLightsAndWaterBeatFlatGrass() {
        List<Poi> pois = PoiScanner.scan(Scenes.village(), Scenes.FEET, 32);
        assertFalse(pois.isEmpty());
        Poi top = pois.get(0);
        assertTrue(top.built() > 10, "top spot should be a build: " + top);
        boolean house = false, water = false;
        for (Poi p : pois) {
            if (p.focus().x() > 6 && p.focus().x() < 16 && p.focus().z() > 4 && p.focus().z() < 14) house = true;
            if (p.water()) water = true;
        }
        assertTrue(house, "the plank house is found");
        assertTrue(water, "the water edge is found");
        double flat = PoiScanner.cell(GridWorld.flat(), 40, 40).score();
        assertTrue(top.score() > flat * 4, "build " + top.score() + " vs flat " + flat);
    }

    @Test
    void spotsAreSpreadOutAndInRange() {
        List<Poi> pois = PoiScanner.scan(Scenes.village(), Scenes.FEET, 32);
        assertTrue(pois.size() <= PoiScanner.KEEP);
        for (int i = 0; i < pois.size(); i++) {
            assertTrue(pois.get(i).focus().horizontal() < 32 + PoiScanner.CELL * 2);
            for (int j = i + 1; j < pois.size(); j++)
                assertTrue(pois.get(i).focus().dist(pois.get(j).focus()) >= PoiScanner.SPACING);
        }
    }

    @Test
    void unloadedChunksAreSkipped() {
        GridWorld small = new GridWorld(64, 6);
        for (Poi p : PoiScanner.scan(small, Scenes.FEET, 32)) assertTrue(Math.abs(p.focus().x()) <= 8);
    }
}
