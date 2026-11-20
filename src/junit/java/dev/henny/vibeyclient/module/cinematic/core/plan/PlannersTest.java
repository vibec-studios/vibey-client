package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Clearance;
import dev.henny.vibeyclient.module.cinematic.core.GridWorld;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import dev.henny.vibeyclient.module.cinematic.core.shot.Shot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannersTest {
    private static final Planner[] OPEN_AIR = {new BuildsPlanner(), new SelfPlanner(), new PanoramaPlanner(),
            new SkyPlanner(), new DroniePlanner()};

    static void assertFair(Scene s, Shot shot) {
        for (double t = 0; t <= shot.duration(); t += 0.05) {
            V3 p = shot.at(t).pos();
            assertTrue(Clearance.free(s.world(), p), "camera in a block at t=" + t + " " + p);
            assertTrue(p.dist(s.eye()) <= s.radius() + 1e-6, "camera too far at t=" + t);
        }
    }

    @Test
    void everyModePlansAFairShotInTheVillage() {
        Scene s = Scenes.of(Scenes.village());
        for (Planner p : OPEN_AIR) {
            for (long seed = 1; seed <= 4; seed++) {
                Shot shot = p.plan(s, new Random(seed));
                assertNotNull(shot, p.getClass().getSimpleName() + " seed " + seed);
                assertFair(s, shot);
            }
        }
    }

    @Test
    void closedRoomGivesNoShotOutside() {
        Scene s = Scenes.of(Scenes.closedRoom());
        for (Planner p : OPEN_AIR) {
            Shot shot = p.plan(s, new Random(3));
            if (shot != null) assertFair(s, shot);
        }
        assertNull(new PanoramaPlanner().plan(s, new Random(1)));
        assertTrue(!new SkyPlanner().possible(s));
    }

    @Test
    void playersBehindWallsAreNeverShown() {
        GridWorld w = GridWorld.flat().fill(-4, 65, 6, 4, 72, 6, GridWorld.STONE);
        Scene.Subject hidden = Scenes.player("hidden", new V3(0.5, 65, 9.5));
        Scene.Subject seen = Scenes.player("seen", new V3(-6.5, 65, -4.5));
        Scene s = Scenes.of(w, List.of(hidden, seen), List.of());
        PlayersPlanner p = new PlayersPlanner();
        assertEquals(List.of(seen), p.candidates(s));
        for (long seed = 1; seed <= 5; seed++) {
            Shot shot = p.plan(s, new Random(seed));
            assertNotNull(shot);
            assertEquals("seen", shot.subject());
            assertFair(s, shot);
        }
        assertTrue(!p.possible(Scenes.of(w, List.of(hidden), List.of())));
    }

    @Test
    void playersTakeTurns() {
        Scene s = Scenes.of(GridWorld.flat(), List.of(Scenes.player("a", new V3(6, 65, 0)),
                Scenes.player("b", new V3(-6, 65, 2))), List.of());
        PlayersPlanner p = new PlayersPlanner();
        Random r = new Random(7);
        String first = p.plan(s, r).subject();
        String second = p.plan(s, r).subject();
        assertTrue(!first.equals(second), first + " twice");
    }

    @Test
    void waypointsInRangeAreToured() {
        Scene s = Scenes.of(GridWorld.flat(), List.of(), List.of(new Scene.Mark("Home", new V3(20, 65, 5)),
                new Scene.Mark("Far", new V3(400, 65, 0))));
        WaypointsPlanner p = new WaypointsPlanner();
        assertEquals(1, p.near(s).size());
        Shot shot = p.plan(s, new Random(2));
        assertNotNull(shot);
        assertEquals("Home", shot.subject());
        assertFair(s, shot);
    }

    @Test
    void speedChangesDuration() {
        Scene slow = Scenes.of(GridWorld.flat());
        Scene fast = new Scene(slow.world(), slow.eye(), slow.feet(), 0, 40, 2, List.of(), List.of(), Scene.Sky.NOON);
        double a = new StillPlanner().plan(slow, new Random(1)).duration();
        double b = new StillPlanner().plan(fast, new Random(1)).duration();
        assertEquals(a / 2, b, 1e-9);
    }
}
