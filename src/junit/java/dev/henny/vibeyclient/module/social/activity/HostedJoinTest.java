package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.module.social.model.Model.Activity;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HostedJoinTest {
    static HostSession session(String address, String world, String joinHost, int joinPort) {
        return new HostSession("h", null, "world", address, "1.21.11", "online", 1, 0, world, joinHost, joinPort);
    }

    static Activity hosting(String world) {
        return new Activity("hosting", null, null, null, null, world, true, null, 0, null, null, 0, null);
    }

    @Test
    void newFieldsWin() {
        HostSession s = session("old.axolite.cc", "Inselwelt", "mossy.axolite.cc", 25570);
        assertEquals("Inselwelt", HostedJoin.world(s, hosting("Presence name")));
        assertEquals("mossy.axolite.cc", HostedJoin.host(s));
        assertEquals(25570, HostedJoin.port(s));
    }

    @Test
    void oldSessionFallsBack() {
        HostSession s = session("mossy.axolite.cc:25566", null, null, 0);
        assertEquals("Presence name", HostedJoin.world(s, hosting("Presence name")));
        assertNull(HostedJoin.world(s, null));
        assertEquals("mossy.axolite.cc", HostedJoin.host(s));
        assertEquals(25566, HostedJoin.port(s));
        HostSession plain = session("mossy.axolite.cc", " ", "", 0);
        assertEquals("mossy.axolite.cc", HostedJoin.host(plain));
        assertEquals(25565, HostedJoin.port(plain));
    }
}
