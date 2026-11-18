package dev.henny.vibeyrules;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesTest {
    @Test
    void configNormalisesAndReportsBadKeys() {
        Rules.Config c = Rules.config(true, List.of("Zoom", "hud.*", "bad*key", "zoom", "*"), " Hi §cthere ", Map.of(), true);
        assertEquals(List.of("zoom", "hud.*", "*"), c.disable());
        assertEquals("Hi there", c.message());
        assertEquals(1, c.problems().size());
    }

    @Test
    void worldOverrideReplacesListAndMessage() {
        Rules.Config c = Rules.config(true, List.of("zoom"), "global", Map.of("World_Nether", Map.of("disable", List.of("hud.*"))), true);
        assertEquals(new Rules.Applied(List.of("hud.*"), "global"), c.rulesFor("world_nether"));
        assertEquals(new Rules.Applied(List.of("zoom"), "global"), c.rulesFor("world"));
        assertTrue(Rules.config(false, List.of("zoom"), "x", Map.of(), true).rulesFor("world").isEmpty());
    }

    @Test
    void encodeMatchesContract() {
        String json = new String(Rules.encode(new Rules.Applied(List.of("zoom", "hud.*"), "No zoom")), StandardCharsets.UTF_8);
        assertEquals("{\"schema\":1,\"disable\":[\"zoom\",\"hud.*\"],\"message\":\"No zoom\"}", json);
        assertEquals("{\"schema\":1,\"disable\":[]}", new String(Rules.encode(Rules.Applied.NONE), StandardCharsets.UTF_8));
    }

    @Test
    void parsesHello() {
        Rules.Hello h = Rules.parseHello("{\"schema\":1,\"client\":\"vibey-client\",\"version\":\"0.3.0\",\"supports\":[\"zoom\",\"hud.cps\",\"BAD KEY\"],\"loader\":\"fabric\"}".getBytes(StandardCharsets.UTF_8));
        assertEquals("vibey-client", h.client());
        assertEquals(List.of("zoom", "hud.cps"), h.supports());
        assertThrows(IllegalArgumentException.class, () -> Rules.parseHello("{\"schema\":2,\"client\":\"x\"}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> Rules.parseHello("nope".getBytes(StandardCharsets.UTF_8)));
        assertNull(Rules.cleanMessage("   "));
    }

    @Test
    void unsupportedKeys() {
        assertEquals(List.of("minimap"), Rules.unsupported(List.of("zoom", "hud.*", "minimap"), List.of("zoom", "hud.cps")));
    }
}
