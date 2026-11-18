package dev.henny.vibeyclient.module.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The 0.2 "lyrics" module becomes "media": settings, on/off state and keybinds are carried over once. */
class MediaMigrationTest {
    @Test
    void movesSettingsStateAndKeybinds(@TempDir Path tmp) throws Exception {
        Path dir = tmp.resolve("config/vibeyclient");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("lyrics.json"), """
                {"position": "HUD", "offsetMs": 300, "_data": {"player": {"skyMode": "SOFT", "sky": "galaxy"}}}""");
        Files.writeString(dir.resolve("modules.json"), "{\"lyrics\": false, \"social\": true}");
        Path options = tmp.resolve("options.txt");
        Files.writeString(options, "key_key.vibeyclient.lyrics.open:key.keyboard.j\nkey_key.vibeyclient.social.open:key.keyboard.g\n");

        assertTrue(MediaMigration.migrate(dir, options));

        JsonObject media = JsonParser.parseString(Files.readString(dir.resolve("media.json"))).getAsJsonObject();
        assertEquals("HUD", media.get("lyrics.position").getAsString());
        assertEquals(300, media.get("lyrics.offsetMs").getAsInt());
        assertEquals("galaxy", media.getAsJsonObject("_data").getAsJsonObject("lyrics").getAsJsonObject("player").get("sky").getAsString());
        assertFalse(Files.exists(dir.resolve("lyrics.json")));
        assertTrue(Files.exists(dir.resolve("lyrics.json.migrated")));

        JsonObject modules = JsonParser.parseString(Files.readString(dir.resolve("modules.json"))).getAsJsonObject();
        assertFalse(modules.has("lyrics"));
        assertFalse(modules.get("media").getAsBoolean());
        assertTrue(modules.get("social").getAsBoolean());

        String opts = Files.readString(options);
        assertTrue(opts.contains("key_key.vibeyclient.media.lyrics.open:key.keyboard.j"), opts);
        assertTrue(opts.contains("key_key.vibeyclient.social.open:key.keyboard.g"), opts);

        // only once: a second start changes nothing
        assertFalse(MediaMigration.migrate(dir, options));
    }

    @Test
    void nothingToDoOnAFreshInstall(@TempDir Path tmp) {
        assertFalse(MediaMigration.migrate(tmp, tmp.resolve("options.txt")));
        assertFalse(Files.exists(tmp.resolve("media.json")));
    }
}
