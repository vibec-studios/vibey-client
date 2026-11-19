package dev.henny.vibeyclient.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.launcher.SessionFile.Snapshot;
import dev.henny.vibeyclient.launcher.SessionFile.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** {@code <instance>/.vibe/session.json} (PLAY-LINKS.md §2): fields per state, atomic write, delete. */
class SessionFileTest {
    private static JsonObject json(Snapshot s) {
        return JsonParser.parseString(SessionFile.serialize(s, 1795040000L)).getAsJsonObject();
    }

    @Test
    void hostingLikeTheContract() {
        JsonObject o = json(new Snapshot(State.HOSTING, "ignored.example", "Neue Welt", "https://vibe.henny.dev/j/k3x9q2", 3, 8));
        assertEquals(1, o.get("schema").getAsInt());
        assertEquals("hosting", o.get("state").getAsString());
        assertEquals("Neue Welt", o.get("world").getAsString());
        assertEquals("https://vibe.henny.dev/j/k3x9q2", o.get("play_link").getAsString());
        assertEquals(3, o.get("players").getAsInt());
        assertEquals(8, o.get("max_players").getAsInt());
        assertEquals(1795040000L, o.get("updated_at").getAsLong());
        assertFalse(o.has("server"), "server only for multiplayer");
    }

    @Test
    void fieldsOnlyWhereTheyApply() {
        JsonObject menu = json(new Snapshot(State.MENU, "a", "b", "c", 1, 2));
        assertEquals(Set.of("schema", "state", "updated_at"), menu.keySet());
        assertEquals("menu", menu.get("state").getAsString());

        JsonObject sp = json(new Snapshot(State.SINGLEPLAYER, "srv", "Welt", "https://x/j/abc", null, null));
        assertEquals(Set.of("schema", "state", "world", "updated_at"), sp.keySet());

        JsonObject mp = json(new Snapshot(State.MULTIPLAYER, "hypixel.net", "w", "l", 120, null));
        assertEquals(Set.of("schema", "state", "server", "players", "updated_at"), mp.keySet());
        assertEquals("hypixel.net", mp.get("server").getAsString());

        // hidden server (server rule discord.server / privacy): simply absent
        JsonObject hidden = json(new Snapshot(State.MULTIPLAYER, null, null, null, null, null));
        assertEquals(Set.of("schema", "state", "updated_at"), hidden.keySet());
        assertEquals("multiplayer", hidden.get("state").getAsString());
    }

    @Test
    void blankAndLongValues() {
        JsonObject o = json(new Snapshot(State.HOSTING, null, "   ", " ", 1, 8));
        assertFalse(o.has("world"));
        assertFalse(o.has("play_link"));
        String longName = "x".repeat(300);
        assertEquals(100, json(new Snapshot(State.SINGLEPLAYER, null, longName, null, null, null)).get("world").getAsString().length());
        assertEquals("multiplayer", json(new Snapshot(State.MULTIPLAYER, "\"quoted\" <tag>", null, null, null, null)).get("state").getAsString());
        assertTrue(SessionFile.serialize(new Snapshot(State.MULTIPLAYER, "<b>", null, null, null, null), 1).contains("\"<b>\""), "no HTML escaping");
        assertEquals(State.MENU, new Snapshot(null, null, null, null, null, null).state());
    }

    @Test
    void writeIsAtomicAndDeleteCleansUp(@TempDir Path dir) throws IOException {
        Path vibe = dir.resolve(".vibe");
        SessionFile.write(vibe, new Snapshot(State.SINGLEPLAYER, null, "Welt", null, null, null), 100);
        Path file = vibe.resolve("session.json");
        assertTrue(Files.isRegularFile(file));
        assertFalse(Files.exists(vibe.resolve("session.json.tmp")), "temp file moved into place");
        SessionFile.write(vibe, new Snapshot(State.HOSTING, null, "Welt", "https://x/j/abcdef", 1, 8), 200);
        JsonObject o = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals("hosting", o.get("state").getAsString());
        assertEquals(200, o.get("updated_at").getAsLong());
        Files.writeString(vibe.resolve("session.json.tmp"), "leftover");
        SessionFile.delete(vibe);
        assertFalse(Files.exists(file));
        assertFalse(Files.exists(vibe.resolve("session.json.tmp")));
        SessionFile.delete(vibe); // twice is fine
        SessionFile.delete(dir.resolve("missing"));
    }
}
