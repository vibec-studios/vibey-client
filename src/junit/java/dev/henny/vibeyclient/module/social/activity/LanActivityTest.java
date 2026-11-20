package dev.henny.vibeyclient.module.social.activity;

import com.google.gson.JsonParser;
import dev.henny.vibeyclient.module.social.model.Model.Activity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanActivityTest {
    static Activity parse(String json) {
        return Activity.from(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void specLanExampleHasNoAddressAndNoGermanTitle() {
        Activity a = parse("{ \"type\": \"playing\", \"server\": \"LAN-Welt\", \"serverKind\": \"lan\", \"joinable\": false, \"addressShared\": false }");
        assertEquals("lan", a.serverKind());
        assertTrue(SharedAddress.onServer(a));
        assertNull(SharedAddress.title(a));
        assertNull(SharedAddress.of(a));
    }

    @Test
    void lanWithNameKeepsTheName() {
        Activity a = parse("{\"type\":\"playing\",\"server\":\"Basement\",\"serverName\":\"Basement\",\"serverKind\":\"lan\"}");
        assertEquals("Basement", SharedAddress.title(a));
        assertNull(SharedAddress.of(a));
    }

    @Test
    void lanNeverUsesAddressHeuristics() {
        Activity a = parse("{\"type\":\"playing\",\"server\":\"192.168.1.5:25565\",\"serverKind\":\"local\",\"joinable\":true,"
                + "\"join\":{\"type\":\"direct\",\"address\":\"192.168.1.5\",\"port\":25565}}");
        assertNull(SharedAddress.of(a));
        assertNull(SharedAddress.title(a));
    }

    @Test
    void remoteAndMissingKindStayAsBefore() {
        Activity a = parse("{\"type\":\"playing\",\"server\":\"play.example.org\",\"serverKind\":\"remote\"}");
        assertEquals("play.example.org", SharedAddress.of(a));
        assertNull(parse("{\"type\":\"playing\"}").serverKind());
    }
}
