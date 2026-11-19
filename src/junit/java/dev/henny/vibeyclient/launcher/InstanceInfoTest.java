package dev.henny.vibeyclient.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** {@code <instance>/.vibe/instance.json} (PLAY-LINKS.md §2): tolerant parsing, manual mod count, play link fields. */
class InstanceInfoTest {
    /** The example from the contract, verbatim. */
    static final String CONTRACT_EXAMPLE = """
            {
              "schema": 1,
              "name": "Cobblemon Abenteuer",
              "mc_version": "1.21.1",
              "loader": { "kind": "fabric", "version": "0.16.9" },
              "pack": { "source": "modrinth", "project_id": "abc", "version_id": "def", "title": "Cobblemon Official" },
              "mods": [ { "source": "modrinth", "project_id": "AANobbMI", "version_id": "xyz" } ],
              "client_pack": "vibey"
            }""";

    /** No modpack: {@code mods} lists everything with a source, with {@code kind}. */
    static final String NO_PACK = """
            {
              "schema": 1, "name": "Meine Welt", "mc_version": "1.21.11",
              "loader": { "kind": "fabric", "version": "0.18.1" },
              "pack": null,
              "mods": [
                { "source": "modrinth", "project_id": "AANobbMI", "version_id": "v1", "kind": "mod", "file": "sodium-0.6.jar" },
                { "source": "curseforge", "project_id": "238222", "version_id": "5101366", "kind": "mod", "file": "jei.jar" },
                { "source": "modrinth", "project_id": "faithful", "kind": "resourcepack", "file": "faithful.zip" },
                { "source": "modrinth", "project_id": "complementary", "kind": "shader" },
                { "source": "github", "project_id": "nope", "kind": "mod" },
                { "source": "modrinth", "project_id": "bad id with spaces", "kind": "mod" },
                { "source": "modrinth", "project_id": "weird", "kind": "plugin" },
                "not an object",
                { "source": "modrinth", "project_id": "dp1", "kind": "datapack" }
              ],
              "client_pack": "vibey",
              "future_field": { "anything": true }
            }""";

    @Test
    void contractExample() {
        InstanceInfo i = InstanceInfo.parse(CONTRACT_EXAMPLE);
        assertTrue(i.present());
        assertEquals(1, i.schema());
        assertEquals("Cobblemon Abenteuer", i.name());
        assertEquals("1.21.1", i.mcVersion());
        assertEquals("fabric", i.loaderKind());
        assertEquals("0.16.9", i.loaderVersion());
        assertEquals(new InstanceInfo.Pack("modrinth", "abc", "def", "Cobblemon Official"), i.pack());
        assertEquals(1, i.mods().size());
        assertEquals(new InstanceInfo.Mod("modrinth", "AANobbMI", "xyz", null, null), i.mods().get(0));
        assertEquals("vibey", i.clientPack());

        JsonObject f = i.playLinkFields();
        assertEquals("abc", f.getAsJsonObject("pack").get("project_id").getAsString());
        assertEquals("Cobblemon Official", f.getAsJsonObject("pack").get("title").getAsString());
        assertEquals(1, f.getAsJsonArray("mods").size());
        assertFalse(f.getAsJsonArray("mods").get(0).getAsJsonObject().has("kind"));
        assertEquals("vibey", f.get("client_pack").getAsString());
    }

    @Test
    void noPackWithKinds() {
        InstanceInfo i = InstanceInfo.parse(NO_PACK);
        assertTrue(i.present());
        assertNull(i.pack());
        // github source, invalid id dropped; unknown kind kept without kind; the string entry skipped
        assertEquals(6, i.mods().size());
        assertEquals("resourcepack", i.mods().get(2).kind());
        assertEquals("shader", i.mods().get(3).kind());
        assertNull(i.mods().get(4).kind());
        assertEquals("datapack", i.mods().get(5).kind());
        assertEquals("sodium-0.6.jar", i.mods().get(0).file());
        assertTrue(i.knownFiles().contains("sodium-0.6.jar"));
        assertTrue(i.knownFiles().contains("jei.jar"));
        assertFalse(i.knownFiles().contains("faithful.zip"), "only mods count for the mods/ folder");

        JsonObject f = i.playLinkFields();
        assertTrue(f.get("pack").isJsonNull());
        JsonArray mods = f.getAsJsonArray("mods");
        assertEquals(6, mods.size());
        for (var m : mods) assertFalse(m.getAsJsonObject().has("file"), "file names are not sent");
        assertEquals("238222", mods.get(1).getAsJsonObject().get("project_id").getAsString());
        assertFalse(mods.get(0).getAsJsonObject().has("version_id") && mods.get(3).getAsJsonObject().has("version_id"));
    }

    @Test
    void missingFileAndGarbage(@TempDir Path dir) throws IOException {
        assertFalse(InstanceInfo.read(dir).present());
        assertEquals(-1, InstanceInfo.read(dir).manualMods(dir.resolve("mods")));
        assertEquals(0, InstanceInfo.read(dir).playLinkFields().size());
        for (String bad : new String[]{"", "   ", "[]", "42", "{not json", "null"}) {
            assertFalse(InstanceInfo.parse(bad).present(), bad);
        }
        Files.createDirectories(dir.resolve(".vibe"));
        Files.writeString(dir.resolve(".vibe/instance.json"), "{\"schema\": 2, \"loader\": \"NeoForge\", \"mods\": {}, \"pack\": 5}");
        InstanceInfo i = InstanceInfo.read(dir);
        assertTrue(i.present(), "newer schema and odd types are tolerated");
        assertEquals(2, i.schema());
        assertEquals("neoforge", i.loaderKind());
        assertNull(i.pack());
        assertTrue(i.mods().isEmpty());
    }

    @Test
    void invalidValuesAreDropped() {
        InstanceInfo i = InstanceInfo.parse("""
                {"loader": {"kind": "rift"}, "pack": {"source": "modrinth", "project_id": "abc"}, "client_pack": "Not Valid!"}""");
        assertNull(i.loaderKind());
        assertNull(i.pack(), "pack without version_id");
        assertNull(i.clientPack());
        assertFalse(i.playLinkFields().has("client_pack"));
    }

    @Test
    void modsAreCappedAt500() {
        StringBuilder sb = new StringBuilder("{\"pack\":null,\"mods\":[");
        for (int n = 0; n < 650; n++) sb.append(n == 0 ? "" : ",").append("{\"source\":\"modrinth\",\"project_id\":\"p").append(n).append("\"}");
        sb.append("]}");
        InstanceInfo i = InstanceInfo.parse(sb.toString());
        assertEquals(650, i.mods().size());
        assertEquals(InstanceInfo.MAX_MODS, i.playLinkFields().getAsJsonArray("mods").size());
    }

    @Test
    void manualModsByFileName(@TempDir Path dir) throws IOException {
        Path mods = Files.createDirectories(dir.resolve("mods"));
        for (String f : new String[]{"sodium-0.6.jar", "JEI.jar", "handmade-a.jar", "handmade-b.jar", "vibeyclient-fabric-1.21.11-0.3.0.jar", "vibey-client-fabric-1.21.11-0.2.1.jar",
                "old.jar.disabled", "notes.txt"}) {
            Files.writeString(mods.resolve(f), "x");
        }
        Files.createDirectories(mods.resolve("folder.jar"));
        InstanceInfo i = InstanceInfo.parse(NO_PACK);
        assertEquals(2, i.manualMods(mods), "handmade-a/b; known names match case-insensitively; our jar, dirs, .disabled ignored");
    }

    @Test
    void manualModsWithKnownFilesAndPack(@TempDir Path dir) throws IOException {
        Path mods = Files.createDirectories(dir.resolve("mods"));
        for (String f : new String[]{"pack-a.jar", "pack-b.jar", "extra.jar"}) Files.writeString(mods.resolve(f), "x");
        InstanceInfo withFiles = InstanceInfo.parse("""
                {"pack": {"source": "modrinth", "project_id": "abc", "version_id": "def"},
                 "known_files": ["mods/pack-a.jar", "mods\\\\pack-b.jar", "resourcepacks/x.zip"]}""");
        assertEquals(1, withFiles.manualMods(mods));
        // a modpack without file names: cannot tell
        assertEquals(-1, InstanceInfo.parse(CONTRACT_EXAMPLE).manualMods(mods));
    }

    @Test
    void manualModsByCountWithoutFileNames(@TempDir Path dir) throws IOException {
        Path mods = Files.createDirectories(dir.resolve("mods"));
        for (String f : new String[]{"a.jar", "b.jar", "c.jar", "d.jar"}) Files.writeString(mods.resolve(f), "x");
        InstanceInfo i = InstanceInfo.parse("""
                {"pack": null, "mods": [
                  {"source": "modrinth", "project_id": "a", "kind": "mod"},
                  {"source": "modrinth", "project_id": "b"},
                  {"source": "modrinth", "project_id": "rp", "kind": "resourcepack"}]}""");
        assertEquals(2, i.manualMods(mods), "4 jars, 2 listed mods (the resource pack does not count)");
        assertEquals(0, i.manualMods(dir.resolve("missing")));
    }
}
