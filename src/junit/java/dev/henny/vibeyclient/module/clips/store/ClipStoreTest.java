package dev.henny.vibeyclient.module.clips.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The clips folder contract (docs/CLIPS.md §10): ids, JSON-last, edits keep unknown fields, JSON-first deletion, cleanup. */
class ClipStoreTest {
    @TempDir
    Path dir;

    private ClipMeta.Info info(String id, Instant at, String reason, String mode, String server, String world) {
        return new ClipMeta.Info(id, at, 31_967, 0, 1280, 720, 30, false, reason, reason.equals("hotkey") ? 0 : 2, 26_100,
                List.of(new ClipMeta.Event(21_400, "kill", "Steve"), new ClipMeta.Event(26_100, "kill", "Kai")),
                "Alex", "1c2d3e4f-0000-0000-0000-000000000000", mode, server, world, "1.21.11", "fabric", "0.4.0", "h264_qsv", true);
    }

    /** A complete clip as the game writes it: video, thumbnail, then the JSON. */
    private String clip(ClipStore s, Instant at, String reason, int videoBytes) throws Exception {
        String id = s.newId(LocalDateTime.ofInstant(at, java.time.ZoneOffset.UTC), reason);
        Files.createDirectories(dir);
        Files.write(dir.resolve(id + ".mp4"), new byte[videoBytes]);
        Files.write(dir.resolve(id + ".jpg"), new byte[10]);
        s.writeJson(id, ClipMeta.json(info(id, at, reason, "multiplayer", "play.example.net", null)));
        return id;
    }

    @Test
    void idsFollowThePatternAndAvoidCollisions() throws Exception {
        ClipStore s = new ClipStore(dir);
        LocalDateTime t = LocalDateTime.of(2026, 11, 19, 21, 4, 33);
        String a = s.newId(t, "multikill");
        assertEquals("2026-11-19_21-04-33_multikill", a);
        Files.createDirectories(dir);
        Files.write(dir.resolve(a + ".mp4"), new byte[1]);
        assertEquals(a + "_2", s.newId(t, "multikill"));
        Files.write(dir.resolve(a + "_2.json"), new byte[1]);
        assertEquals(a + "_3", s.newId(t, "multikill"));
        assertTrue(ClipStore.ID.matcher(a + "_3").matches());
        assertEquals("2026-11-19_21-04-33_clip", s.newId(t, "!!"));
    }

    @Test
    void jsonFollowsTheContract() {
        JsonObject mp = ClipMeta.json(info("2026-11-19_21-04-33_multikill", Instant.parse("2026-11-19T20:04:33.512Z"),
                "multikill", "multiplayer", "play.example.net", "should not appear"));
        assertEquals(1, mp.get("schema").getAsInt());
        assertEquals("2026-11-19T20:04:33Z", mp.get("created_at").getAsString());
        assertEquals("2026-11-19_21-04-33_multikill.mp4", mp.get("video").getAsString());
        assertEquals("play.example.net", mp.get("server").getAsString());
        assertFalse(mp.has("world"), "world only for singleplayer/hosting");
        assertFalse(mp.has("audio_codec"), "no sound: no audio_codec");
        assertEquals(2, mp.get("kills").getAsInt());
        assertEquals(2, mp.getAsJsonArray("events").size());
        assertFalse(mp.get("kept").getAsBoolean());
        assertEquals("vibey-client", mp.get("client").getAsString());

        JsonObject sp = ClipMeta.json(info("2026-11-19_21-04-33_hotkey", Instant.now(), "hotkey", "singleplayer", "x", "My World"));
        assertFalse(sp.has("server"), "server only for multiplayer");
        assertEquals("My World", sp.get("world").getAsString());
        assertFalse(sp.has("kills") || sp.has("events") || sp.has("highlight_ms"), "auto fields only on auto clips");
        for (var e : sp.entrySet()) assertFalse(e.getValue().isJsonNull(), "no nulls: " + e.getKey());

        JsonObject hidden = ClipMeta.json(info("2026-11-19_21-04-33_kill", Instant.now(), "kill", "multiplayer", null, null));
        assertFalse(hidden.has("server"), "a hidden server address is left out");
    }

    @Test
    void listsCompleteClipsNewestFirstAndSkipsTheRest() throws Exception {
        ClipStore s = new ClipStore(dir);
        String old = clip(s, Instant.parse("2026-11-18T10:00:00Z"), "kill", 100);
        String newer = clip(s, Instant.parse("2026-11-19T10:00:00Z"), "hotkey", 100);
        // a save in progress: video, no JSON
        Files.write(dir.resolve("2026-11-19_12-00-00_kill.mp4"), new byte[5]);
        // JSON whose video is missing
        String ghost = "2026-11-19_13-00-00_kill";
        s.writeJson(ghost, ClipMeta.json(info(ghost, Instant.now(), "kill", "multiplayer", null, null)));
        // a newer schema we don't know
        String future = "2026-11-19_14-00-00_kill";
        Files.write(dir.resolve(future + ".mp4"), new byte[5]);
        JsonObject f = ClipMeta.json(info(future, Instant.now(), "kill", "multiplayer", null, null));
        f.addProperty("schema", 2);
        s.writeJson(future, f);
        // dotfiles and junk
        Files.writeString(dir.resolve(".2026-11-19_15-00-00_kill.json.tmp"), "{");
        Files.writeString(dir.resolve("notes.json"), "{}");

        List<ClipStore.Clip> l = s.list();
        assertEquals(2, l.size());
        assertEquals(newer, l.get(0).id());
        assertEquals(old, l.get(1).id());
        assertNotNull(l.get(0).thumbnail());
        assertFalse(l.get(0).auto());
        assertTrue(l.get(1).auto());
    }

    @Test
    void editsKeepUnknownFieldsAndClampUserData() throws Exception {
        ClipStore s = new ClipStore(dir);
        String id = clip(s, Instant.now(), "kill", 100);
        Path json = dir.resolve(id + ".json");
        JsonObject o = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
        o.addProperty("launcher_note", "keep me");
        Files.writeString(json, o.toString(), StandardCharsets.UTF_8);

        ClipStore.Clip c = s.edit(id, true, "  Bridge 1v2  " + "x".repeat(200), List.of("bedwars", " bedwars ", "", "y".repeat(40)));
        assertTrue(c.kept());
        assertEquals(100, c.title().length());
        assertEquals(List.of("bedwars", "y".repeat(32)), c.tags());
        JsonObject after = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
        assertEquals("keep me", after.get("launcher_note").getAsString());
        assertEquals(2, after.get("kills").getAsInt());
        assertFalse(Files.exists(dir.resolve("." + id + ".json.tmp")));

        c = s.edit(id, null, "", List.of());
        assertNull(c.title());
        assertTrue(c.kept(), "null leaves kept as it was");
        assertFalse(JsonParser.parseString(Files.readString(json)).getAsJsonObject().has("tags"));
        assertNull(s.edit("2026-01-01_00-00-00_kill", true, null, null), "gone");
    }

    @Test
    void deleteRemovesTheJsonFirstAndAllFiles() throws Exception {
        ClipStore s = new ClipStore(dir);
        String id = clip(s, Instant.now(), "kill", 100);
        assertTrue(s.delete(id));
        assertFalse(Files.exists(dir.resolve(id + ".json")));
        assertFalse(Files.exists(dir.resolve(id + ".mp4")));
        assertFalse(Files.exists(dir.resolve(id + ".jpg")));
        assertFalse(s.delete("../../etc"), "only ids");
    }

    @Test
    void cleanupDeletesOldestUnkeptAndNeverKept() throws Exception {
        ClipStore s = new ClipStore(dir);
        String a = clip(s, Instant.parse("2026-11-01T00:00:00Z"), "kill", 1000);
        String b = clip(s, Instant.parse("2026-11-02T00:00:00Z"), "kill", 1000);
        String c = clip(s, Instant.parse("2026-11-03T00:00:00Z"), "kill", 1000);
        s.edit(a, true, null, null); // the oldest is kept
        long one = s.get(b).filesBytes();
        ClipStore.Cleanup r = s.cleanup(one * 2 + 10);
        assertEquals(1, r.deleted());
        assertNotNull(s.get(a), "kept survives");
        assertNull(s.get(b), "oldest unkept goes first");
        assertNotNull(s.get(c));
        assertFalse(r.keptOverLimit());

        r = s.cleanup(10);
        assertNotNull(s.get(a), "kept is never deleted");
        assertNull(s.get(c));
        assertTrue(r.keptOverLimit());
    }

    @Test
    void orphansAndOldPartFilesGoAtStart() throws Exception {
        ClipStore s = new ClipStore(dir);
        String keep = clip(s, Instant.now(), "kill", 10);
        Files.write(dir.resolve("2026-11-19_12-00-00_kill.mp4"), new byte[5]);
        Files.write(dir.resolve(".2026-11-19_12-00-01_kill.mp4.part"), new byte[5]);
        Files.write(dir.resolve(".2026-11-19_12-00-02_kill.mp4.part"), new byte[5]);
        Files.write(dir.resolve("holiday.mp4"), new byte[5]); // not ours
        Files.createDirectories(s.tmpDir().resolve("x"));
        FileTime old = FileTime.from(Instant.now().minusSeconds(7200));
        Files.setLastModifiedTime(dir.resolve("2026-11-19_12-00-00_kill.mp4"), old);
        Files.setLastModifiedTime(dir.resolve(".2026-11-19_12-00-01_kill.mp4.part"), old);
        int n = s.removeOrphans(System.currentTimeMillis());
        assertEquals(2, n);
        assertTrue(Files.exists(dir.resolve(".2026-11-19_12-00-02_kill.mp4.part")), "a fresh part file may still be written");
        assertTrue(Files.exists(dir.resolve("holiday.mp4")));
        assertNotNull(s.get(keep));
        assertFalse(Files.exists(s.tmpDir()));
    }
}
