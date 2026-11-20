package dev.henny.vibeyclient.module.social.presence;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerFieldsTest {
    @Test
    void parsesHostAndPort() {
        assertEquals(new HostPort("play.example.org", 25565), HostPort.parse("play.example.org"));
        assertEquals(new HostPort("play.example.org", 25570), HostPort.parse(" play.example.org:25570 "));
        assertEquals(new HostPort("2001:db8::1", 25565), HostPort.parse("2001:db8::1"));
        assertEquals(new HostPort("2001:db8::1", 25599), HostPort.parse("[2001:db8::1]:25599"));
        assertNull(HostPort.parse("host:99999"));
        assertNull(HostPort.parse("host:abc"));
        assertNull(HostPort.parse(""));
        assertNull(HostPort.parse(null));
    }

    @Test
    void publicServerIsSent() {
        JsonObject a = new JsonObject();
        ServerFields.add(a, "play.example.org:25570", "  Example SMP ");
        assertEquals("Example SMP", a.get("serverName").getAsString());
        assertEquals("play.example.org", a.get("serverAddress").getAsString());
        assertEquals(25570, a.get("serverPort").getAsInt());
    }

    @Test
    void lanServerKeepsOnlyTheName() {
        JsonObject a = new JsonObject();
        ServerFields.add(a, "192.168.0.20:25565", "Home");
        assertEquals("Home", a.get("serverName").getAsString());
        assertTrue(a.get("serverAddress").isJsonNull());
        assertTrue(a.get("serverPort").isJsonNull());
        JsonObject b = new JsonObject();
        ServerFields.add(b, "localhost", null);
        assertTrue(b.get("serverName").isJsonNull());
        assertTrue(b.get("serverAddress").isJsonNull());
    }

    @Test
    void invalidHostsAreNotSent() {
        assertNull(ServerFields.shareable("bad host.example.org"));
        assertNull(ServerFields.shareable("-x.example.org"));
        assertEquals(new HostPort("2001:db8::1", 25565), ServerFields.shareable("[2001:db8::1]"));
    }

    @Test
    void longNamesAreClipped() {
        assertEquals(100, ServerFields.name("x".repeat(150)).length());
        assertNull(ServerFields.name("  "));
    }
}
