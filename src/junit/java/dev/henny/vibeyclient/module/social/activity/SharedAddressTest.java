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
        return new Activity("playing", null, "1.21.11", "fabric", server, null, joinable, join, 0);
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
    void hostShapes() {
        assertTrue(SharedAddress.looksLikeHost("localhost:25565"));
        assertTrue(SharedAddress.looksLikeHost("127.0.0.1"));
        assertFalse(SharedAddress.looksLikeHost("hypixel"));
        assertFalse(SharedAddress.looksLikeHost("a b.c"));
    }
}
