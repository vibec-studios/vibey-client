package dev.henny.vibeyclient.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Backend tokens per Minecraft account and backend URL in {@code <gameDir>/vibeyclient/auth.json} – deliberately not
 * in {@code config/}, which modpacks ship and share. Written atomically right after every login/refresh, because the
 * refresh token rotates: losing the newest one means logging in again, reusing an old one ends the session.
 */
public final class AuthStore {
    /** Stored login of one account. {@code expiresAt}: access token expiry in epoch millis. */
    public record Tokens(String accessToken, String refreshToken, long expiresAt, String sessionId) {
        public static Tokens fromPair(JsonObject pair) {
            int expiresIn = J.num(pair, "expiresIn", 900);
            return new Tokens(J.str(pair, "accessToken"), J.str(pair, "refreshToken"),
                    System.currentTimeMillis() + expiresIn * 1000L, J.str(pair, "sessionId"));
        }

        public boolean valid() {
            return accessToken != null && refreshToken != null;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private JsonObject root;

    public AuthStore(Path file) {
        this.file = file;
    }

    public static AuthStore defaultStore() {
        return new AuthStore(Vibey.platform().gameDir().resolve("vibeyclient").resolve("auth.json"));
    }

    public static String key(String baseUrl, String accountId) {
        return baseUrl + "|" + accountId;
    }

    private JsonObject accounts() {
        if (root == null) {
            root = new JsonObject();
            try {
                if (Files.isRegularFile(file)) {
                    JsonObject read = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (read != null) root = read;
                }
            } catch (Exception e) {
                Vibey.LOG.warn("Could not read {}: {}", file, e.toString());
            }
            root.addProperty("version", 1);
            if (!root.has("accounts") || !root.get("accounts").isJsonObject()) root.add("accounts", new JsonObject());
            // 0.3.0: logins of https://vibe.henny.dev belong to https://vibe.axolite.app now (same server and database)
            boolean had = root.getAsJsonObject("accounts").size() > 0;
            if (DomainMove.migrateAuthFile(root) && had) save();
        }
        return root.getAsJsonObject("accounts");
    }

    private JsonObject entry(String key, boolean create) {
        JsonObject a = accounts();
        JsonObject e = J.obj(a, key);
        if (e == null && create) {
            e = new JsonObject();
            a.add(key, e);
        }
        return e;
    }

    public synchronized Tokens get(String key) {
        JsonObject e = entry(key, false);
        if (e == null || J.str(e, "refreshToken") == null) return null;
        return new Tokens(J.str(e, "accessToken"), J.str(e, "refreshToken"), J.lng(e, "expiresAt", 0), J.str(e, "sessionId"));
    }

    /** Saves (or with {@code null} forgets) the tokens and writes the file before returning. */
    public synchronized void put(String key, Tokens t, String name) {
        JsonObject e = entry(key, true);
        if (t == null) {
            e.remove("accessToken");
            e.remove("refreshToken");
            e.remove("expiresAt");
            e.remove("sessionId");
        } else {
            e.addProperty("accessToken", t.accessToken());
            e.addProperty("refreshToken", t.refreshToken());
            e.addProperty("expiresAt", t.expiresAt());
            e.addProperty("sessionId", t.sessionId());
            e.addProperty("loggedOut", false);
            e.addProperty("everLoggedIn", true);
        }
        if (name != null) e.addProperty("name", name);
        save();
    }

    /** The player logged out on purpose: no automatic login for this account until they log in again. */
    public synchronized void setLoggedOut(String key, boolean loggedOut) {
        entry(key, true).addProperty("loggedOut", loggedOut);
        save();
    }

    public synchronized boolean loggedOut(String key) {
        return J.bool(entry(key, false), "loggedOut", false);
    }

    public synchronized boolean everLoggedIn(String key) {
        return J.bool(entry(key, false), "everLoggedIn", false);
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            restrict(tmp);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Vibey.LOG.error("Could not save {}: {}", file, e.toString());
        }
    }

    /** Owner-only file where the file system supports it (Linux/macOS). */
    private static void restrict(Path p) {
        try {
            Set<java.nio.file.attribute.PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(p, perms);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows: the game directory is in the user profile anyway
        }
    }
}
