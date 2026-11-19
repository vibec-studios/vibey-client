package dev.henny.cosmetico.anim;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.model.Bundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotePlayTest {
    private static Bundle emote(boolean loop, boolean hold) {
        var o = Fixtures.emote("t:e");
        var main = o.getAsJsonObject("animations").getAsJsonObject("main");
        main.addProperty("loop", loop);
        main.addProperty("hold", hold);
        return Fixtures.parse(o);
    }

    @Test
    void fadesInOverFifteenHundredths() {
        EmotePlay e = new EmotePlay("t:e", emote(false, false), 0);
        e.update(0);
        assertEquals(0f, e.weight, 1e-6f);
        e.update(EmotePlay.FADE);
        assertEquals(1f, e.weight, 1e-6f);
        e.update(EmotePlay.FADE / 2);
        assertEquals(0.5f, e.weight, 1e-6f);
    }

    @Test
    void onceStopsAtTheEndAndFinishesAfterTheFade() {
        EmotePlay e = new EmotePlay("t:e", emote(false, false), 0);
        e.update(1.0);
        assertTrue(e.stopping());
        assertFalse(e.finished(1.1));
        assertTrue(e.finished(1.2));
    }

    @Test
    void holdAndLoopKeepPlaying() {
        EmotePlay hold = new EmotePlay("t:e", emote(false, true), 0);
        hold.update(5);
        assertFalse(hold.stopping());
        assertEquals(1f, hold.localTime, 1e-6f);
        EmotePlay loop = new EmotePlay("t:e", emote(true, false), 0);
        loop.update(5.25);
        assertFalse(loop.stopping());
        assertEquals(0.25f, loop.localTime, 1e-5f);
    }

    @Test
    void stopFadesOut() {
        EmotePlay e = new EmotePlay("t:e", emote(true, false), 0);
        e.update(0.5);
        e.stop(0.5);
        e.update(0.5 + EmotePlay.FADE / 2);
        assertEquals(0.5f, e.weight, 1e-5f);
        assertTrue(e.finished(0.7));
    }

    @Test
    void headChannelIsSampledInBlockbenchUnits() {
        EmotePlay e = new EmotePlay("t:e", emote(false, true), 0);
        e.update(0.5);
        assertEquals(5f, e.pose[0], 1e-5f);
        assertEquals(1, e.channels[0]);
        assertEquals(1f, e.pose[6 + 9], 1e-6f);
    }
}
