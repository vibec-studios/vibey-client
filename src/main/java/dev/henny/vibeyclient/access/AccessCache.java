package dev.henny.vibeyclient.access;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AccessCache {
    public static final long TTL_MS = 7L * 24 * 60 * 60 * 1000;
    static final long SKEW_MS = 5 * 60 * 1000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private JsonObject approved;

    public AccessCache(Path file) {
        this.file = file;
    }

    public synchronized boolean fresh(String key, long now) {
        if (key == null) return false;
        JsonElement at = entries().get(key);
        if (at == null || !at.isJsonPrimitive() || !at.getAsJsonPrimitive().isNumber()) return false;
        long t = at.getAsLong();
        return t <= now + SKEW_MS && now - t < TTL_MS;
    }

    public synchronized void remember(String key, long now) {
        if (key == null) return;
        entries().addProperty(key, now);
        save();
    }

    public synchronized void forget(String key) {
        if (key == null || !entries().has(key)) return;
        entries().remove(key);
        save();
    }

    private JsonObject entries() {
        if (approved != null) return approved;
        approved = new JsonObject();
        try {
            if (Files.isRegularFile(file)) {
                JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (root.isJsonObject() && root.getAsJsonObject().get("approved") instanceof JsonObject o) approved = o;
            }
        } catch (Exception e) {
            Vibey.LOG.warn("Could not read {}: {}", file, e.toString());
        }
        return approved;
    }

    private void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("approved", approved);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Vibey.LOG.warn("Could not save {}: {}", file, e.toString());
        }
    }
}
