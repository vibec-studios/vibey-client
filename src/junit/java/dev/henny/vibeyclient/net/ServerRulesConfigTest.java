package dev.henny.vibeyclient.net;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRulesConfigTest {
    @Test
    void writesAndReadsTheDefault(@TempDir Path dir) {
        Path f = dir.resolve("config").resolve("vibeyclient-server.json");
        ServerRulesConfig.Loaded l = ServerRulesConfig.load(f);
        assertNull(l.error());
        assertTrue(Files.exists(f));
        assertTrue(l.config().enabled());
        assertTrue(l.config().disable().isEmpty());
        assertTrue(l.config().rulesFor("minecraft:overworld", UUID.randomUUID(), "x").isEmpty());
    }

    @Test
    void worldsAndBypass() {
        UUID staff = UUID.fromString("11111111-2222-3333-4444-555555555555");
        ServerRulesConfig c = ServerRulesConfig.parse("""
                { "disable": ["Zoom", "hud.*"], "message": "No zoom",
                  "worlds": { "the_nether": { "disable": ["freelook"] }, "minecraft:the_end": { "message": "End" } },
                  "bypass": ["Staffie", "11111111-2222-3333-4444-555555555555"] }""");
        UUID u = UUID.randomUUID();
        assertEquals(new RulesProtocol.Rules(List.of("zoom", "hud.*"), "No zoom"), c.rulesFor("minecraft:overworld", u, "Alex"));
        assertEquals(new RulesProtocol.Rules(List.of("freelook"), "No zoom"), c.rulesFor("minecraft:the_nether", u, "Alex"));
        assertEquals(new RulesProtocol.Rules(List.of("zoom", "hud.*"), "End"), c.rulesFor("minecraft:the_end", u, "Alex"));
        assertTrue(c.rulesFor("minecraft:overworld", u, "staffie").isEmpty());
        assertTrue(c.rulesFor("minecraft:overworld", staff, "Other").isEmpty());
        assertTrue(ServerRulesConfig.parse("{\"enabled\":false,\"disable\":[\"zoom\"]}").rulesFor(null, u, "a").isEmpty());
    }

    @Test
    void badFilesFallBackToNoRules(@TempDir Path dir) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ServerRulesConfig.parse("{\"disable\":[\"bad key\"]}"));
        assertThrows(IllegalArgumentException.class, () -> ServerRulesConfig.parse("{\"disable\":\"zoom\"}"));
        assertThrows(IllegalArgumentException.class, () -> ServerRulesConfig.parse("{\"enabled\":\"yes\"}"));
        Path f = dir.resolve("vibeyclient-server.json");
        Files.writeString(f, "{ broken");
        ServerRulesConfig.Loaded l = ServerRulesConfig.load(f);
        assertNotNull(l.error());
        assertEquals(ServerRulesConfig.EMPTY, l.config());
    }
}
