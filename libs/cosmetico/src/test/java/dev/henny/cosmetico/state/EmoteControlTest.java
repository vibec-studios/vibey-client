package dev.henny.cosmetico.state;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.state.EmoteListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmoteControlTest {
    private final TestEngine t = new TestEngine();
    private final UUID player = UUID.randomUUID();

    @Test
    void unknownOrCosmeticIdsDoNotPlay() {
        t.add(Fixtures.parse(Fixtures.cosmetic("t:hat", "hat")));
        assertFalse(t.engine.emotes.play(player, "t:nope"));
        assertFalse(t.engine.emotes.play(player, "t:hat"));
    }

    @Test
    void playStopEndWithListeners() {
        t.add(Fixtures.parse(Fixtures.emote("t:e")));
        List<String> ev = new ArrayList<>();
        t.engine.emotes.listen(new EmoteListener() {
            @Override
            public void started(UUID p, String id) {
                ev.add("start " + id);
            }

            @Override
            public void stopped(UUID p, String id) {
                ev.add("stop " + id);
            }

            @Override
            public void ended(UUID p, String id) {
                ev.add("end " + id);
            }
        });
        assertTrue(t.engine.emotes.play(player, "t:e"));
        assertEquals("t:e", t.engine.emotes.playing(player));
        t.time.advance(0.3);
        t.engine.emotes.stop(player);
        assertNull(t.engine.emotes.playing(player));
        t.time.advance(0.2);
        t.engine.emotes.tick(t.engine.avatars.get(player), t.engine.clock.now());
        assertNull(t.engine.avatars.get(player).emote);
        assertEquals(List.of("start t:e", "stop t:e", "end t:e"), ev);
    }

    @Test
    void onceEmotesEndByThemselves() {
        t.add(Fixtures.parse(Fixtures.emote("t:e")));
        t.engine.emotes.play(player, "t:e");
        t.time.advance(1.2);
        t.engine.emotes.tick(t.engine.avatars.get(player), t.engine.clock.now());
        assertNull(t.engine.avatars.get(player).emote);
    }

    @Test
    void pruneKeepsPlayingAvatars() {
        t.add(Fixtures.parse(Fixtures.emote("t:e")));
        t.engine.emotes.play(player, "t:e");
        t.engine.avatars.get(UUID.randomUUID());
        t.engine.avatars.prune(1000, 600);
        assertEquals(1, t.engine.avatars.all().size());
    }
}
