package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Clearance;
import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectorTest {
    @Test
    void longRunStaysFairInEveryMode() {
        Scene s = Scenes.of(Scenes.village(), List.of(Scenes.player("p", new V3(-5.5, 65, -3.5))),
                List.of(new Scene.Mark("Mine", new V3(18, 65, -6))));
        for (Mode m : Mode.values()) {
            Director d = new Director(42);
            d.mode(m);
            for (int i = 0; i < 60 * 40; i++) {
                Pose p = d.update(s, 1 / 60.0);
                assertTrue(p.pos().dist(s.eye()) <= s.radius() + 1e-6, m + " too far");
                assertTrue(Clearance.free(s.world(), p.pos()), m + " inside a block at frame " + i);
                assertTrue(d.fade() >= 0 && d.fade() <= 1);
            }
            assertTrue(d.shots() >= 2, m + " should have moved on to another shot");
        }
    }

    @Test
    void fallsBackToStillInAClosedRoom() {
        Scene s = Scenes.of(Scenes.closedRoom());
        Director d = new Director(1);
        d.mode(Mode.PANORAMA);
        Pose p = d.update(s, 0.1);
        assertTrue(p.pos().dist(s.eye()) < 1.5);
    }

    @Test
    void mixSwitchesModes() {
        Scene s = Scenes.of(Scenes.village(), List.of(Scenes.player("p", new V3(-5.5, 65, -3.5))), List.of());
        Director d = new Director(9);
        d.mixSeconds(10);
        Set<Mode> seen = EnumSet.noneOf(Mode.class);
        for (int i = 0; i < 60 * 90; i++) {
            d.update(s, 1 / 60.0);
            seen.add(d.playing());
        }
        assertTrue(seen.size() >= 4, "modes seen: " + seen);
        assertEquals(Mode.MIX, d.chosen());
    }

    @Test
    void fadesInAndOutAroundEveryShot() {
        Scene s = Scenes.of(Scenes.village());
        Director d = new Director(5);
        d.mode(Mode.SELF);
        d.update(s, 0.0);
        assertTrue(d.fade() > 0.95, "starts black");
        for (int i = 0; i < 120; i++) d.update(s, 1 / 60.0);
        assertTrue(d.fade() < 0.01, "clear after the fade in");
        d.mode(Mode.BUILDS);
        for (int i = 0; i < 18; i++) d.update(s, 1 / 60.0);
        assertTrue(d.fade() > 0.6, "mode change cuts through black, fade " + d.fade());
    }
}
