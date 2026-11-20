package dev.henny.r3.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.henny.r3.hint.SetHintStore;
import dev.henny.r3.input.GestureBindings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConfigStore {
    public static final String HINTS = "hints.json";
    public static final String BINDINGS = "bindings.json";
    public static final String WORLDS = "worlds";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path dir;

    public ConfigStore(Path dir) {
        this.dir = dir;
    }

    public Path dir() {
        return dir;
    }

    public SetHintStore loadHints() {
        return SetHintStore.fromJson(TextFiles.read(dir.resolve(HINTS)));
    }

    public boolean saveHints(SetHintStore hints) {
        return TextFiles.write(dir.resolve(HINTS), hints.toJson());
    }

    public GestureBindings loadBindings(GestureBindings into) {
        String json = TextFiles.read(dir.resolve(BINDINGS));
        if (json == null || json.isBlank()) return into;
        try {
            Map<String, String> map = GSON.fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
            return map == null ? into : into.load(map);
        } catch (JsonParseException e) {
            return into;
        }
    }

    public boolean saveBindings(GestureBindings bindings) {
        return TextFiles.write(dir.resolve(BINDINGS), GSON.toJson(bindings.toMap()));
    }

    public PlacementStore loadWorld(String worldKey) {
        Path file = worldFile(worldKey);
        String json = TextFiles.read(file);
        PlacementStore store = json == null ? null : PlacementStore.parse(json);
        if (store != null) return store;
        if (Files.exists(file)) TextFiles.keepBroken(file);
        return new PlacementStore();
    }

    public boolean saveWorld(String worldKey, PlacementStore store) {
        return TextFiles.write(worldFile(worldKey), store.toJson());
    }

    public Path worldFile(String worldKey) {
        return dir.resolve(WORLDS).resolve(fileName(worldKey));
    }

    public static String fileName(String worldKey) {
        String safe = worldKey.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.length() > 60) safe = safe.substring(0, 60);
        return safe + "-" + Integer.toHexString(worldKey.hashCode()) + ".json";
    }
}
