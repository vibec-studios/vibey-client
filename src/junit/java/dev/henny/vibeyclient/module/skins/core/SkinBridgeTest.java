package dev.henny.vibeyclient.module.skins.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinBridgeTest {
    @TempDir
    Path vibe;

    static final String LIBRARY = """
            {"schema":1,"updated_at":1795040000,
             "account":{"uuid":"1c2d3e4f-0000-4000-8000-00000000abcd","name":"Alex","kind":"microsoft"},
             "can_change":true,
             "active":{"library_id":"ab12cd34ef56","sha1":"aa","variant":"slim"},
             "skins":[
               {"id":"ab12cd34ef56","name":"Ritter","variant":"slim","file":"ab12cd34ef56.png","sha1":"aa","added_at":5},
               {"id":"ff00ff00ff00","name":"","variant":"classic","file":"ff00ff00ff00.png","sha1":"bb","added_at":4},
               {"id":"../evil","name":"x","file":"../evil.png"},
               {"id":"ok1","name":"x","file":"C:/Windows/evil.png"},
               {"id":"ok2","name":"future","file":"ok2.png","variant":"wide","extra":true}
             ]}""";

    @Test
    void parsesTheLibraryAndDropsUnsafeEntries() {
        SkinLibrary l = SkinLibrary.parse(LIBRARY);
        assertNotNull(l);
        assertEquals("Alex", l.accountName());
        assertTrue(l.canChange());
        assertEquals(3, l.skins().size());
        assertEquals("Skin", l.skins().get(1).name());
        assertTrue(l.skins().get(0).slim());
        assertFalse(l.skins().get(2).slim());
        assertTrue(l.isActive(l.skins().get(0)));
        assertFalse(l.isActive(l.skins().get(1)));
        SkinLibrary changed = l.withActive("ff00ff00ff00", false);
        assertTrue(changed.isActive(changed.skins().get(1)));
    }

    @Test
    void unknownSchemaOrJunkIsNull() {
        assertNull(SkinLibrary.parse("{\"schema\":2,\"skins\":[]}"));
        assertNull(SkinLibrary.parse("not json"));
        assertNull(SkinLibrary.parse("[]"));
    }

    @Test
    void activeBySha1WhenNotInLibrary() {
        SkinLibrary l = SkinLibrary.parse("""
                {"schema":1,"can_change":false,"reason":"offline_account","active":{"sha1":"BB"},
                 "skins":[{"id":"a","name":"A","file":"a.png","sha1":"bb"}]}""");
        assertNotNull(l);
        assertFalse(l.canChange());
        assertEquals("offline_account", l.reason());
        assertTrue(l.isActive(l.skins().get(0)));
    }

    @Test
    void requestIsWrittenAtomically() throws Exception {
        BridgeFiles.writeRequest(vibe, new SkinRequest("req-1", "ab12cd34ef56", true, 1795040000));
        assertFalse(Files.exists(vibe.resolve(BridgeFiles.REQUEST + ".tmp")));
        JsonObject o = JsonParser.parseString(Files.readString(vibe.resolve(BridgeFiles.REQUEST))).getAsJsonObject();
        assertEquals(1, o.get("schema").getAsInt());
        assertEquals("req-1", o.get("id").getAsString());
        assertEquals("apply", o.get("action").getAsString());
        assertEquals("ab12cd34ef56", o.get("skin_id").getAsString());
        assertEquals("slim", o.get("variant").getAsString());
        assertEquals(1795040000, o.get("requested_at").getAsLong());
    }

    @Test
    void resultsParse() throws Exception {
        Files.writeString(vibe.resolve(BridgeFiles.RESULT), """
                {"schema":1,"id":"req-1","status":"ok","skin_id":"ab12cd34ef56","variant":"slim","finished_at":1}""");
        SkinResult r = BridgeFiles.readResult(vibe);
        assertEquals(SkinResult.Status.OK, r.status());
        assertTrue(r.slim());
        assertNull(SkinResult.parse("{\"schema\":1,\"id\":\"x\",\"status\":\"maybe\"}"));
        SkinResult e = SkinResult.parse("{\"schema\":1,\"id\":\"x\",\"status\":\"error\",\"error\":\"rate_limited\",\"message\":\"Zu oft\"}");
        assertEquals("rate_limited", e.error());
        assertEquals("Zu oft", e.message());
        assertEquals(SkinResult.Status.WORKING, SkinResult.parse("{\"schema\":1,\"id\":\"x\",\"status\":\"working\"}").status());
    }

    @Test
    void pngIsReadFromTheSkinsFolderOnly() throws Exception {
        Files.writeString(vibe.resolve(BridgeFiles.LIBRARY), LIBRARY, StandardCharsets.UTF_8);
        SkinLibrary l = BridgeFiles.readLibrary(vibe);
        LibrarySkin s = l.skins().get(0);
        assertNull(BridgeFiles.readPng(vibe, s));
        Files.createDirectories(vibe.resolve(BridgeFiles.SKIN_DIR));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        Files.write(vibe.resolve(BridgeFiles.SKIN_DIR).resolve(s.file()), png);
        assertArrayEquals(png, BridgeFiles.readPng(vibe, s));
        BridgeFiles.writeRequest(vibe, new SkinRequest("r", "a", null, 1));
        BridgeFiles.clearOwn(vibe);
        assertFalse(Files.exists(vibe.resolve(BridgeFiles.REQUEST)));
        assertTrue(Files.exists(vibe.resolve(BridgeFiles.LIBRARY)));
    }
}
