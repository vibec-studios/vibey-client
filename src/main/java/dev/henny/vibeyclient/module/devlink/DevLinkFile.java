package dev.henny.vibeyclient.module.devlink;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * {@code <gameDir>/vibeyclient/devlink.json} ({@code { "port": 47821, "token": "…", "pid": 1234 }}) so the Blockbench
 * plugin can find a running game, plus token generation. Plain Java, no game classes.
 */
public final class DevLinkFile {
    private DevLinkFile() {}

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 32 URL-safe characters (192 random bits). */
    public static String newToken() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** Writes the file atomically (temp file + move). */
    public static void write(Path file, int port, String token, long pid) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("port", port);
        o.addProperty("token", token);
        o.addProperty("pid", pid);
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + "." + pid + ".tmp");
        Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(o) + "\n", StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Deletes the file if it is ours (same pid), so a second game instance's file is left alone. */
    public static void delete(Path file, long pid) {
        try {
            if (!Files.exists(file)) return;
            JsonElement e = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (e.isJsonObject() && e.getAsJsonObject().has("pid") && e.getAsJsonObject().get("pid").getAsLong() != pid) return;
            Files.deleteIfExists(file);
        } catch (IOException | RuntimeException ex) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }
}
