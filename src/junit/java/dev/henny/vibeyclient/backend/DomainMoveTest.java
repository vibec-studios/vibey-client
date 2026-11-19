package dev.henny.vibeyclient.backend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.api.setting.SettingsGroup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/** Domain move henny.dev → axolite (vibe-launcher docs/DOMAINS.md): stored old defaults move once, own values stay. */
class DomainMoveTest {
    @Test
    void backendExactOldDefault() {
        assertEquals("https://vibe.axolite.app", DomainMove.migrateBackend("https://vibe.henny.dev"));
        assertEquals("https://vibe.axolite.app", DomainMove.migrateBackend("https://vibe.henny.dev/"));
        assertEquals("https://vibe.axolite.app", DomainMove.migrateBackend("  https://vibe.henny.dev//  "));
        assertEquals("https://vibe.axolite.app", DomainMove.migrateBackend("HTTPS://VIBE.HENNY.DEV"));
    }

    @Test
    void backendCustomUntouched() {
        assertNull(DomainMove.migrateBackend("http://127.0.0.1:3100"));
        assertNull(DomainMove.migrateBackend("https://vibe.axolite.app"));
        assertNull(DomainMove.migrateBackend("https://vibe.henny.dev/api"), "a path is not the default");
        assertNull(DomainMove.migrateBackend("http://vibe.henny.dev"), "plain http is not the default");
        assertNull(DomainMove.migrateBackend("https://staging.vibe.henny.dev"));
        assertNull(DomainMove.migrateBackend(""));
        assertNull(DomainMove.migrateBackend(null));
    }

    @Test
    void relay() {
        assertEquals("relay.axolite.cc:25565", DomainMove.migrateRelay("relay.vibey.henny.dev:25565"));
        assertEquals("relay.axolite.cc:25565", DomainMove.migrateRelay("relay.vibey.henny.dev"));
        assertEquals("relay.axolite.cc:25565", DomainMove.migrateRelay("vibey.henny.dev:25565"));
        assertEquals("relay.axolite.cc:25565", DomainMove.migrateRelay("vibey.henny.dev"));
        assertEquals("relay.axolite.cc:25565", DomainMove.migrateRelay(" Relay.Vibey.Henny.Dev.:25565 "));
        assertEquals("relay.axolite.cc:26400", DomainMove.migrateRelay("relay.vibey.henny.dev:26400"), "own port kept");
        assertNull(DomainMove.migrateRelay("127.0.0.1:26400"));
        assertNull(DomainMove.migrateRelay("relay.axolite.cc:25565"));
        assertNull(DomainMove.migrateRelay("my-relay.example.org"));
        assertNull(DomainMove.migrateRelay("mossy-otter-27.vibey.henny.dev"), "a world address is not the relay");
        assertNull(DomainMove.migrateRelay("vibey.henny.dev:abc"));
        assertNull(DomainMove.migrateRelay(null));
    }

    @Test
    void productionUnderBothNames() {
        assertTrue(DomainMove.isProduction("https://vibe.henny.dev"));
        assertTrue(DomainMove.isProduction("https://vibe.axolite.app/"));
        assertTrue(DomainMove.isProduction("https://VIBE.AXOLITE.APP:443"));
        assertTrue(DomainMove.isProduction("vibe.axolite.app"));
        assertTrue(DomainMove.isProduction("https://vibe.henny.dev./"));
        assertFalse(DomainMove.isProduction("http://127.0.0.1:3100"));
        assertFalse(DomainMove.isProduction("http://localhost:3300"));
        assertFalse(DomainMove.isProduction("https://axolite.app"));
        assertFalse(DomainMove.isProduction(null));
    }

    @Test
    void coreFile() {
        JsonObject json = json("{\"language\":\"GERMAN\",\"backendUrl\":\"https://vibe.henny.dev/\",\"_data\":{\"onboarded\":true}}");
        assertTrue(DomainMove.migrateCore(json));
        assertEquals("https://vibe.axolite.app", json.get("backendUrl").getAsString());
        assertEquals("GERMAN", json.get("language").getAsString());

        JsonObject custom = json("{\"backendUrl\":\"http://localhost:3100\"}");
        assertFalse(DomainMove.migrateCore(custom));
        assertEquals("http://localhost:3100", custom.get("backendUrl").getAsString());
        assertFalse(DomainMove.migrateCore(json("{}")));
    }

    @Test
    void multiplayerFile() {
        JsonObject json = json("{\"relay\":\"relay.vibey.henny.dev:25565\",\"_data\":{\"host\":{\"relay\":\"relay.vibey.henny.dev:25565\",\"maxPlayers\":8}}}");
        assertTrue(DomainMove.migrateMultiplayer(json));
        assertEquals("relay.axolite.cc:25565", json.get("relay").getAsString());
        assertEquals("relay.axolite.cc:25565", json.getAsJsonObject("_data").getAsJsonObject("host").get("relay").getAsString());
        JsonObject custom = json("{\"relay\":\"127.0.0.1:26400\"}");
        assertFalse(DomainMove.migrateMultiplayer(custom));
        assertEquals("127.0.0.1:26400", custom.get("relay").getAsString());
    }

    /** SettingsGroup.migrate: runs once, records the id, a value set again afterwards stays. */
    @Test
    void settingsMigrationIdempotent() {
        Map<String, Predicate<JsonObject>> m = new LinkedHashMap<>();
        m.put(DomainMove.ID, DomainMove::migrateCore);
        JsonObject json = json("{\"backendUrl\":\"https://vibe.henny.dev\"}");
        assertTrue(SettingsGroup.migrate(json, m));
        assertEquals("https://vibe.axolite.app", json.get("backendUrl").getAsString());
        assertTrue(DomainMove.done(json.getAsJsonObject("_data"), DomainMove.ID));

        // second start: nothing runs
        assertFalse(SettingsGroup.migrate(json, m));
        // the player deliberately goes back to the old address: it stays
        json.addProperty("backendUrl", "https://vibe.henny.dev");
        assertFalse(SettingsGroup.migrate(json, m));
        assertEquals("https://vibe.henny.dev", json.get("backendUrl").getAsString());
        assertEquals(1, json.getAsJsonObject("_data").getAsJsonArray("migrations").size());

        // a custom value is recorded as migrated too and never touched
        JsonObject custom = json("{\"backendUrl\":\"http://127.0.0.1:3100\",\"_data\":{\"onboarded\":true}}");
        assertTrue(SettingsGroup.migrate(custom, m));
        assertEquals("http://127.0.0.1:3100", custom.get("backendUrl").getAsString());
        assertTrue(custom.getAsJsonObject("_data").get("onboarded").getAsBoolean());
        assertTrue(DomainMove.done(custom.getAsJsonObject("_data"), DomainMove.ID));
    }

    @Test
    void authAccountsMoved() {
        JsonObject root = json("{\"version\":1,\"accounts\":{"
                + "\"https://vibe.henny.dev|1111\":{\"refreshToken\":\"r1\",\"name\":\"A\"},"
                + "\"https://vibe.henny.dev|2222\":{\"refreshToken\":\"old\"},"
                + "\"https://vibe.axolite.app|2222\":{\"refreshToken\":\"new\"},"
                + "\"http://127.0.0.1:3100|1111\":{\"refreshToken\":\"local\"}}}");
        assertTrue(DomainMove.migrateAuthFile(root));
        JsonObject a = root.getAsJsonObject("accounts");
        assertEquals("r1", a.getAsJsonObject("https://vibe.axolite.app|1111").get("refreshToken").getAsString());
        assertEquals("new", a.getAsJsonObject("https://vibe.axolite.app|2222").get("refreshToken").getAsString(), "newer entry wins");
        assertEquals("local", a.getAsJsonObject("http://127.0.0.1:3100|1111").get("refreshToken").getAsString());
        assertFalse(a.has("https://vibe.henny.dev|1111"));
        assertFalse(a.has("https://vibe.henny.dev|2222"));
        assertEquals(3, a.size());
        // once: a later login for the old backend (chosen on purpose) stays where it is
        a.add("https://vibe.henny.dev|3333", json("{\"refreshToken\":\"r3\"}"));
        assertFalse(DomainMove.migrateAuthFile(root));
        assertTrue(a.has("https://vibe.henny.dev|3333"));
    }

    /** AuthStore reads auth.json of 0.2.x: the login is found under the new backend URL and the file is rewritten. */
    @Test
    void authStoreFile() throws Exception {
        Path dir = Files.createTempDirectory("vibey-auth");
        Path f = dir.resolve("auth.json");
        Files.writeString(f, "{\"version\":1,\"accounts\":{\"https://vibe.henny.dev|abc\":{\"accessToken\":\"a\",\"refreshToken\":\"r\",\"expiresAt\":5,\"sessionId\":\"s\",\"everLoggedIn\":true}}}",
                StandardCharsets.UTF_8);
        AuthStore store = new AuthStore(f);
        AuthStore.Tokens t = store.get(AuthStore.key("https://vibe.axolite.app", "abc"));
        assertNotNull(t);
        assertEquals("r", t.refreshToken());
        assertNull(store.get(AuthStore.key("https://vibe.henny.dev", "abc")));
        assertTrue(store.everLoggedIn(AuthStore.key("https://vibe.axolite.app", "abc")));
        JsonObject saved = json(Files.readString(f, StandardCharsets.UTF_8));
        assertTrue(saved.getAsJsonObject("accounts").has("https://vibe.axolite.app|abc"));
        assertTrue(DomainMove.done(saved, DomainMove.ID));
        // read again (next start): same result, nothing moved twice
        AuthStore again = new AuthStore(f);
        assertNotNull(again.get(AuthStore.key("https://vibe.axolite.app", "abc")));
    }

    @Test
    void loadoutKeyBackend() {
        assertEquals("https://vibe.axolite.app", DomainMove.cachedBackend("https://vibe.henny.dev"));
        assertEquals("http://127.0.0.1:3100", DomainMove.cachedBackend("http://127.0.0.1:3100"));
    }

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }
}
