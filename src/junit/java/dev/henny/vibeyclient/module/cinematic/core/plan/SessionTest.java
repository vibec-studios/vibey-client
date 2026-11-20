package dev.henny.vibeyclient.module.cinematic.core.plan;

import dev.henny.vibeyclient.module.cinematic.core.Pose;
import dev.henny.vibeyclient.module.cinematic.core.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTest {
    private static final Scene SCENE = Scenes.of(Scenes.village());
    private static final Pose PLAYER = new Pose(SCENE.eye(), 30, 10, 70);

    private static Session.Frame run(Session s, double seconds) {
        Session.Frame f = null;
        for (int i = 0; i < Math.round(seconds * 60); i++) f = s.update(SCENE, PLAYER, 1 / 60.0);
        return f;
    }

    @Test
    void introDipsToBlackThenRuns() {
        Session s = new Session(new Director(3));
        Session.Frame f = s.update(SCENE, PLAYER, 0);
        assertEquals(PLAYER, f.pose());
        assertEquals(0, f.fade(), 1e-9);
        f = run(s, 0.55);
        assertTrue(f.fade() > 0.9 && f.bars() > 0.9);
        f = run(s, 3);
        assertEquals(Session.Phase.RUN, f.phase());
        assertTrue(f.fade() < 0.05);
    }

    @Test
    void stopFliesBackToThePlayer() {
        Session s = new Session(new Director(3));
        run(s, 4);
        s.stop(SCENE);
        assertTrue(s.phase() == Session.Phase.FLY_BACK || s.phase() == Session.Phase.FADE_BACK);
        Session.Frame f = run(s, 1.5);
        assertTrue(f.done());
        assertEquals(PLAYER, f.pose());
        assertEquals(0, f.bars(), 1e-9);
        assertEquals(0, f.fade(), 1e-9);
    }

    @Test
    void stopDuringIntroFadesStraightBack() {
        Session s = new Session(new Director(3));
        run(s, 0.3);
        s.stop(SCENE);
        assertEquals(Session.Phase.FADE_BACK, s.phase());
        Session.Frame f = run(s, 0.8);
        assertTrue(f.done());
        assertTrue(!s.running());
    }

    @Test
    void flyBackMovesInAStraightLine() {
        Session s = new Session(new Director(11));
        run(s, 5);
        s.stop(SCENE);
        if (s.phase() != Session.Phase.FLY_BACK) return;
        V3 start = s.update(SCENE, PLAYER, 0).pose().pos();
        double prev = start.dist(PLAYER.pos());
        for (int i = 0; i < 60; i++) {
            double d = s.update(SCENE, PLAYER, 1 / 60.0).pose().pos().dist(PLAYER.pos());
            assertTrue(d <= prev + 1e-9);
            prev = d;
        }
    }
}
