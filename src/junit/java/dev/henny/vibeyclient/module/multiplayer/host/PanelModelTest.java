package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelModelTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void parsesPanel() {
        PanelModel.State s = PanelModel.state("hs1", json("""
                {"policy":{"privacy":"friends","defaultRole":"guest","maxPlayers":5},
                 "members":[{"uuid":"0f3c6a1e2b4d4c8e9a7b112233445566","name":"Mia","role":"member","user":null,"addedAt":"x"}],
                 "kicks":[{"uuid":null,"name":"Griefer","reason":"no","permanent":true,"at":"x"}]}"""));
        assertEquals("friends", s.policy().privacy());
        assertEquals(5, s.policy().maxPlayers());
        assertTrue(s.member("0f3c6a1e-2b4d-4c8e-9a7b-112233445566", "Other"));
        assertTrue(s.member(null, "mia"));
        assertFalse(s.member("aaaaaaaa-2b4d-4c8e-9a7b-112233445566", "Mia"));
        assertTrue(s.kicks().get(0).permanent());
        assertNull(PanelModel.state("x", json("{\"policy\":{\"privacy\":\"public\",\"defaultRole\":\"guest\",\"maxPlayers\":null}}"))
                .policy().maxPlayers());
    }

    @Test
    void parsesDecisionsAndKicks() {
        PanelModel.Decision ok = PanelModel.decision(json("{\"allow\":true,\"role\":\"member\"}"));
        assertTrue(ok.allow());
        assertEquals("member", ok.role());
        PanelModel.Decision no = PanelModel.decision(json("{\"allow\":false,\"code\":\"not_invited\",\"message\":\"m\"}"));
        assertFalse(no.allow());
        assertEquals("not_invited", no.code());
        assertTrue(PanelModel.decision(null).allow());

        PanelModel.KickOrder k = PanelModel.kick(json("""
                {"sessionId":"hs1","hostId":"u1","player":{"uuid":"0f3c6a1e2b4d4c8e9a7b112233445566","name":"Bob"},"reason":null,"permanent":false}"""));
        assertEquals("Bob", k.name());
        assertNull(k.reason());
        assertEquals("0f3c6a1e-2b4d-4c8e-9a7b-112233445566", HostPanel.dashed(k.uuid()));
    }

    @Test
    void readsSvcPort(@TempDir Path dir) throws Exception {
        Path cfg = dir.resolve("voicechat-server.properties");
        assertEquals(24454, Voice.svcPort(cfg, 50000));
        Files.writeString(cfg, "# Simple Voice Chat\nport=24500\nvoice_host=\n");
        assertEquals(24500, Voice.svcPort(cfg, 50000));
        Files.writeString(cfg, "port=-1\n");
        assertEquals(50000, Voice.svcPort(cfg, 50000));
    }
}
