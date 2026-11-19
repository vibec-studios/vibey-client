package dev.henny.vibeyclient.backend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.net.LoadoutToken;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * The backend's loadout signing key ({@code loadoutSigning} of {@code GET /api/v1/meta}, VIBE-API-2 §6), cached on
 * disk per backend URL so tokens of other players can be checked while the backend is unreachable or online
 * features are off. {@code loadoutSigning: null} (or a backend without the field) clears it: then no token is
 * valid and server-channel loadouts only show items everyone has. Thread-safe.
 */
public final class LoadoutKeys {
    private LoadoutKeys() {}

    private static volatile LoadoutToken.SigningKey key;
    /** Backend URL the key belongs to. */
    private static volatile String keyBackend;
    private static volatile boolean diskRead;
    private static volatile int version;

    /** The key for the configured backend, or null. */
    public static LoadoutToken.SigningKey current() {
        readDisk();
        LoadoutToken.SigningKey k = key;
        return k != null && norm(BackendClient.configuredBaseUrl()).equals(keyBackend) ? k : null;
    }

    /** Bumped whenever the key changes (receivers re-check stored tokens). */
    public static int version() {
        return version;
    }

    /** Takes {@code loadoutSigning} from a meta response of {@code baseUrl}. */
    public static void fromMeta(String baseUrl, JsonElement meta) {
        if (!(meta instanceof JsonObject o)) return;
        readDisk();
        JsonElement signing = o.get("loadoutSigning");
        LoadoutToken.SigningKey k = LoadoutToken.parseKey(signing);
        if (k == null && signing != null && !signing.isJsonNull())
            Vibey.LOG.warn("Backend {} sent an unusable loadoutSigning key; signed loadouts are off", baseUrl);
        set(norm(baseUrl), k, true);
    }

    private static synchronized void set(String backend, LoadoutToken.SigningKey k, boolean save) {
        LoadoutToken.SigningKey old = Objects.equals(backend, keyBackend) ? key : null;
        boolean same = old == null ? k == null : k != null && old.keyId().equals(k.keyId());
        key = k;
        keyBackend = backend;
        if (same) return;
        version++;
        if (k != null) Vibey.LOG.info("Loadout signing key {} of {}", k.keyId(), backend);
        if (save) save(backend, k);
    }

    private static Path file() {
        return Vibey.platform().gameDir().resolve("vibeyclient").resolve("cache").resolve("loadout-key.json");
    }

    private static void save(String backend, LoadoutToken.SigningKey k) {
        try {
            Path f = file();
            if (k == null) {
                Files.deleteIfExists(f);
                return;
            }
            JsonObject o = new JsonObject();
            o.addProperty("backend", backend);
            JsonObject s = new JsonObject();
            s.addProperty("alg", "Ed25519");
            s.addProperty("keyId", k.keyId());
            s.addProperty("publicKey", k.publicKeyBase64());
            o.add("loadoutSigning", s);
            Files.createDirectories(f.getParent());
            Files.writeString(f, o.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Vibey.LOG.debug("Could not cache the loadout signing key: {}", e.toString());
        }
    }

    private static void readDisk() {
        if (diskRead) return;
        synchronized (LoadoutKeys.class) {
            if (diskRead) return;
            diskRead = true;
            try {
                Path f = file();
                if (!Files.isRegularFile(f)) return;
                JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
                LoadoutToken.SigningKey k = LoadoutToken.parseKey(o.get("loadoutSigning"));
                if (k != null && o.has("backend") && keyBackend == null) set(norm(DomainMove.cachedBackend(o.get("backend").getAsString())), k, false);
            } catch (Exception e) {
                Vibey.LOG.debug("Could not read the cached loadout signing key: {}", e.toString());
            }
        }
    }

    private static String norm(String url) {
        String s = url == null ? "" : url.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
