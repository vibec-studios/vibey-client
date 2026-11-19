package dev.henny.vibeyclient.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRulesTest {
    @AfterEach
    void reset() {
        ServerRules.clear();
    }

    @Test
    void exactAndWildcardMatching() {
        ServerRules.apply(List.of("zoom", "hud.*"), "msg");
        assertTrue(ServerRules.isDisabled("zoom"));
        assertTrue(ServerRules.isDisabled("hud.cps"));
        assertTrue(ServerRules.isDisabled("hud.coords"));
        assertFalse(ServerRules.isDisabled("hud"), "hud.* only covers keys below hud.");
        assertFalse(ServerRules.isDisabled("hudx.cps"));
        assertFalse(ServerRules.isDisabled("zoomies"));
        assertFalse(ServerRules.isDisabled("freelook"));
        assertFalse(ServerRules.isDisabled(null));
        assertEquals("msg", ServerRules.message());
    }

    @Test
    void starDisablesEverything() {
        ServerRules.apply(List.of("*"), null);
        assertTrue(ServerRules.isDisabled("emotes"));
        assertTrue(ServerRules.isDisabled("cosmetics.pets"));
        assertNull(ServerRules.message());
    }

    @Test
    void groupWildcard() {
        ServerRules.apply(List.of("cosmetics.*"), "  ");
        assertTrue(ServerRules.isDisabled("cosmetics.pets"));
        assertTrue(ServerRules.isDisabled("cosmetics.wings"));
        assertFalse(ServerRules.isDisabled("emotes"));
        assertNull(ServerRules.message(), "a blank message counts as none");
    }

    @Test
    void dropsMalformedRulesAndClears() {
        ServerRules.apply(List.of("hu*d", "*x", "", "ok"), null);
        assertEquals(List.of("ok"), ServerRules.active());
        AtomicInteger fired = new AtomicInteger();
        ServerRules.Listener l = fired::incrementAndGet;
        ServerRules.listen(l);
        ServerRules.clear();
        ServerRules.clear(); // no change, no event
        ServerRules.unlisten(l);
        assertEquals(1, fired.get());
        assertFalse(ServerRules.isDisabled("ok"));
    }
}
