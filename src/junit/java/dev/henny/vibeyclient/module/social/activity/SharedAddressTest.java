package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.module.social.model.Model.Activity;
import dev.henny.vibeyclient.module.social.model.Model.Join;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedAddressTest {
    static Activity server(String server, boolean joinable, Join join) {
        return new Activity("playing", null, "1.21.11", "fabric", server, null, joinable, join, 0, null, null, 0, null, null);
    }

    static Activity details(String server, String name, String address, int port, Boolean shared) {
        return new Activity("playing", null, "1.21.11", "fabric", server, null, false, null, 0, name, address, port, shared, null);
    }

    @Test
    void hiddenServerIsNotAnAddress() {
        assertNull(SharedAddress.of(server("Multiplayer", false, null)));
        assertNull(SharedAddress.of(server("My cool server", false, null)));
        assertNull(SharedAddress.of(null));
    }

    @Test
    void addressFromServerOrJoin() {
        assertEquals("play.example.net", SharedAddress.of(server("play.example.net", false, null)));
        assertEquals("mc.example.org:25570", SharedAddress.of(server("mc.example.org:25570", false, null)));
        assertEquals("relay.x:25566", SharedAddress.of(server("Multiplayer", true, new Join("relay", "relay.x", 25566, null))));
        assertEquals("relay.x", SharedAddress.of(server(null, true, new Join("relay", "relay.x", 25565, null))));
        assertNull(SharedAddress.of(server(null, false, new Join("relay", "relay.x", 25565, null))));
    }

    @Test
    void addressSharedWinsOverHeuristic() {
        assertEquals("play.example.org:25570", SharedAddress.of(details("play.example.org", "Example SMP", "play.example.org", 25570, true)));
        assertEquals("play.example.org", SharedAddress.of(details("x", null, "play.example.org", 25565, true)));
        assertEquals("[2001:db8::1]:25570", SharedAddress.of(details(null, null, "2001:db8::1", 25570, true)));
        assertNull(SharedAddress.of(details("play.example.org", "Example SMP", null, 0, false)));
        assertNull(SharedAddress.of(details("play.example.org", null, "play.example.org", 0, false)));
        assertNull(SharedAddress.of(details("Example SMP", "Example SMP", null, 0, true)));
    }

    @Test
    void titlePrefersServerName() {
        assertEquals("Example SMP", SharedAddress.title(details("Example SMP", "Example SMP", null, 0, false)));
        assertNull(SharedAddress.title(details("Multiplayer", null, null, 0, false)));
        assertEquals("play.example.org:25570", SharedAddress.title(details("play.example.org", null, "play.example.org", 25570, true)));
        assertEquals("play.example.net", SharedAddress.title(server("play.example.net", false, null)));
        assertEquals("Survival Island", SharedAddress.title(server("Survival Island", false, null)));
        assertNull(SharedAddress.title(server("Multiplayer", false, null)));
    }

    @Test
    void onServerNeedsAnyServerField() {
        assertTrue(SharedAddress.onServer(details(null, "Example SMP", null, 0, false)));
        assertTrue(SharedAddress.onServer(server("Multiplayer", false, null)));
        assertFalse(SharedAddress.onServer(details(null, " ", null, 0, null)));
    }

    @Test
    void hostShapes() {
        assertTrue(SharedAddress.looksLikeHost("localhost:25565"));
        assertTrue(SharedAddress.looksLikeHost("127.0.0.1"));
        assertFalse(SharedAddress.looksLikeHost("hypixel"));
        assertFalse(SharedAddress.looksLikeHost("a b.c"));
    }
}
