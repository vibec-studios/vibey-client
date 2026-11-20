package dev.henny.vibeyclient.module.social.presence;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerKindTest {
    @Test
    void ownWorldIsLocalOnlyWhenOpened() {
        assertEquals("local", ServerKind.of(true, true, null));
        assertNull(ServerKind.of(true, false, null));
        assertEquals("local", ServerKind.of(true, true, "play.example.org"));
    }

    @Test
    void privateAddressesAreLan() {
        for (String ip : new String[] {"192.168.0.20", "10.0.0.5:25570", "172.16.4.1", "127.0.0.1", "localhost", "[::1]:25565",
                "fe80::1", "169.254.3.3", "minecraft.local", "nas.fritz.box.lan", "192-168-1-5.nip.io", "server", "fd00::7"}) {
            assertEquals("lan", ServerKind.of(false, false, ip), ip);
        }
    }

    @Test
    void publicAddressesAreRemote() {
        for (String ip : new String[] {"play.example.org", "mc.example.net:25570", "8.8.8.8", "[2001:db8::1]:25599", "1-2-3-4.nip.io"}) {
            assertEquals("remote", ServerKind.of(false, false, ip), ip);
        }
    }

    @Test
    void unparsableIsLanAndNothingIsNull() {
        assertEquals("lan", ServerKind.of(false, false, "host:99999"));
        assertNull(ServerKind.of(false, false, null));
        assertNull(ServerKind.of(false, false, " "));
    }

    @Test
    void lanFlag() {
        assertTrue(ServerKind.lan("lan"));
        assertTrue(ServerKind.lan("local"));
        assertFalse(ServerKind.lan("remote"));
        assertFalse(ServerKind.lan(null));
    }

    @Test
    void lanServerIsSentWithoutAddress() {
        JsonObject a = new JsonObject();
        ServerFields.add(a, "192.168.0.20:25570", null);
        assertEquals("lan", a.get("serverKind").getAsString());
        assertEquals("LAN-Welt", a.get("server").getAsString());
        assertTrue(a.get("serverAddress").isJsonNull());
        assertTrue(a.get("serverPort").isJsonNull());
        assertFalse(a.toString().contains("192.168"));
        JsonObject named = new JsonObject();
        ServerFields.add(named, "10.0.0.2", "Basement");
        assertEquals("Basement", named.get("server").getAsString());
        assertEquals("Basement", named.get("serverName").getAsString());
    }

    @Test
    void remoteServerIsMarked() {
        JsonObject a = new JsonObject();
        ServerFields.add(a, "play.example.org:25570", "Example SMP");
        assertEquals("remote", a.get("serverKind").getAsString());
        assertEquals("play.example.org:25570", a.get("server").getAsString());
        assertEquals("play.example.org", a.get("serverAddress").getAsString());
    }

    @Test
    void localWorldKeepsItsName() {
        JsonObject a = new JsonObject();
        ServerFields.local(a, " Inselwelt ");
        assertEquals("local", a.get("serverKind").getAsString());
        assertEquals("Inselwelt", a.get("serverName").getAsString());
        assertFalse(a.has("serverAddress"));
    }
}
