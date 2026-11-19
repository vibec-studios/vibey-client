package dev.henny.vibeyclient.net;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesProtocolTest {
    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void parsesTheContractExample() throws Exception {
        RulesProtocol.Rules r = RulesProtocol.parseRules(b("""
                { "schema": 1, "disable": ["zoom", "freelook", "minimap", "hud.cps", "hud.coords", "hud.*", "emotes", "cosmetics.pets", "discord.server"],
                  "message": "Auf diesem Server sind Zoom und Koordinaten aus." }"""));
        assertEquals(List.of("zoom", "freelook", "minimap", "hud.cps", "hud.coords", "hud.*", "emotes", "cosmetics.pets", "discord.server"), r.disable());
        assertEquals("Auf diesem Server sind Zoom und Koordinaten aus.", r.message());
    }

    @Test
    void skipsInvalidKeysAndNormalises() throws Exception {
        RulesProtocol.Rules r = RulesProtocol.parseRules(b("{\"schema\":1,\"disable\":[\"Zoom\",\"zoom\",\"hud*\",\"*.hud\",\"a..b\",5,null,\"hud.*\",\"*\",\" freelook \"]}"));
        assertEquals(List.of("zoom", "hud.*", "*", "freelook"), r.disable());
        assertNull(r.message());
    }

    @Test
    void rejectsWrongShapes() {
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(b("{\"disable\":[]}")));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(b("{\"schema\":2,\"disable\":[]}")));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(b("{\"schema\":\"1\"}")));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(b("{\"schema\":1,\"disable\":\"zoom\"}")));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(b("{\"schema\":1,\"message\":{}}")));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(b("[1]")));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(b("not json {")));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(new byte[0]));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(null));
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseRules(new byte[RulesProtocol.MAX_BYTES + 1]));
    }

    @Test
    void limitsCountAndMessage() throws Exception {
        StringBuilder sb = new StringBuilder("{\"schema\":1,\"disable\":[");
        for (int i = 0; i < 400; i++) sb.append(i == 0 ? "" : ",").append("\"k").append(i).append('"');
        sb.append("],\"message\":\"").append("x".repeat(1000)).append("\"}");
        RulesProtocol.Rules r = RulesProtocol.parseRules(b(sb.toString()));
        assertEquals(RulesProtocol.MAX_RULES, r.disable().size());
        assertEquals(RulesProtocol.MAX_MESSAGE, r.message().length());
        assertEquals("No zoom here", RulesProtocol.cleanMessage(" §cNo\u0007 zoom\nhere "));
        assertNull(RulesProtocol.cleanMessage(" \t "));
    }

    @Test
    void roundTrips() throws Exception {
        RulesProtocol.Rules r = new RulesProtocol.Rules(List.of("zoom", "hud.*"), "Hi");
        assertEquals("{\"schema\":1,\"disable\":[\"zoom\",\"hud.*\"],\"message\":\"Hi\"}", new String(RulesProtocol.encodeRules(r), StandardCharsets.UTF_8));
        assertEquals(r, RulesProtocol.parseRules(RulesProtocol.encodeRules(r)));
        assertTrue(RulesProtocol.parseRules(b("{\"schema\":1}")).isEmpty());
    }

    @Test
    void hello() throws Exception {
        byte[] json = RulesProtocol.encodeHello("vibey-client", "0.3.0", List.of("zoom", "emotes"), "1.21.11", "fabric");
        assertEquals("{\"schema\":1,\"client\":\"vibey-client\",\"version\":\"0.3.0\",\"supports\":[\"zoom\",\"emotes\"],\"minecraft\":\"1.21.11\",\"loader\":\"fabric\"}",
                new String(json, StandardCharsets.UTF_8));
        RulesProtocol.Hello h = RulesProtocol.parseHello(json);
        assertEquals(new RulesProtocol.Hello("vibey-client", "0.3.0", List.of("zoom", "emotes")), h);
        assertThrows(RulesProtocol.BadPacket.class, () -> RulesProtocol.parseHello(b("{\"schema\":1,\"client\":\"bad client name\"}")));
    }
}
