package dev.henny.vibeyclient.module.social.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewFieldsParseTest {
    static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void oldActivityHasNoDetails() {
        Model.Activity a = Model.Activity.from(json("{\"type\":\"playing\",\"server\":\"play.example.org\",\"joinable\":false}"));
        assertEquals("play.example.org", a.server());
        assertNull(a.serverName());
        assertNull(a.serverAddress());
        assertEquals(0, a.serverPort());
        assertNull(a.addressShared());
    }

    @Test
    void newActivityShared() {
        Model.Activity a = Model.Activity.from(json("{\"type\":\"playing\",\"server\":\"play.example.org\",\"serverName\":\"Example SMP\","
                + "\"serverAddress\":\"play.example.org\",\"serverPort\":25570,\"addressShared\":true}"));
        assertEquals("Example SMP", a.serverName());
        assertEquals("play.example.org", a.serverAddress());
        assertEquals(25570, a.serverPort());
        assertTrue(a.addressShared());
    }

    @Test
    void newActivityHidden() {
        Model.Activity a = Model.Activity.from(json("{\"type\":\"playing\",\"server\":\"Example SMP\",\"serverName\":\"Example SMP\","
                + "\"joinable\":false,\"addressShared\":false,\"serverPort\":null}"));
        assertFalse(a.addressShared());
        assertNull(a.serverAddress());
        assertEquals(0, a.serverPort());
    }

    @Test
    void hostSessionOldAndNew() {
        Model.HostSession old = Model.HostSession.from(json("{\"id\":\"h1\",\"address\":\"mossy.axolite.cc\",\"status\":\"online\"}"));
        assertNull(old.worldName());
        assertNull(old.joinHost());
        assertEquals(0, old.joinPort());
        Model.HostSession s = Model.HostSession.from(json("{\"id\":\"h2\",\"address\":\"mossy.axolite.cc:25570\",\"status\":\"online\","
                + "\"worldName\":\"Inselwelt\",\"joinHost\":\"mossy.axolite.cc\",\"joinPort\":25570}"));
        assertEquals("Inselwelt", s.worldName());
        assertEquals("mossy.axolite.cc", s.joinHost());
        assertEquals(25570, s.joinPort());
    }

    @Test
    void inviteDestination() {
        Model.Invite old = Model.Invite.from(json("{\"id\":\"i1\",\"kind\":\"world\",\"target\":{\"name\":\"Farm\"}}"));
        assertNull(old.destination());
        assertEquals("Farm", old.displayName());
        Model.Invite nul = Model.Invite.from(json("{\"id\":\"i2\",\"target\":{\"name\":\"Farm\"},\"destination\":null}"));
        assertNull(nul.destination());
        Model.Invite i = Model.Invite.from(json("{\"id\":\"i3\",\"kind\":\"server\",\"target\":{\"name\":\"T\"},"
                + "\"destination\":{\"host\":\"play.example.org\",\"port\":25570,\"name\":\"Example SMP\"}}"));
        assertEquals(new Destination("play.example.org", 25570, "Example SMP"), i.destination());
        assertEquals("Example SMP", i.displayName());
        Model.Invite noPort = Model.Invite.from(json("{\"id\":\"i4\",\"destination\":{\"host\":\"play.example.org\"}}"));
        assertEquals(25565, noPort.destination().port());
        assertNull(noPort.destination().name());
    }

    @Test
    void privacyShareDefaultsOff() {
        assertFalse(Model.Privacy.from(json("{\"showHosting\":true}")).shareServerAddress());
        assertFalse(Model.Privacy.from(null).shareServerAddress());
        assertTrue(Model.Privacy.from(json("{\"shareServerAddress\":true}")).shareServerAddress());
    }
}
