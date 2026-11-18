package dev.henny.vibeyclient.api.setting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Tasks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The settings of one module (or the global settings), persisted as {@code config/vibeyclient/<id>.json}.
 * Besides the typed settings a module may keep free-form data in {@link #data()} (stored under {@code "_data"}),
 * call {@link #markDirty()} after changing it. Saving happens on a background thread, debounced.
 */
public final class SettingsGroup {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final String id;
    private final List<Setting<?, ?>> settings = new ArrayList<>();
    private JsonObject data = new JsonObject();
    private boolean loaded;

    public SettingsGroup(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public <S extends Setting<?, ?>> S add(S setting) {
        for (Setting<?, ?> s : settings) {
            if (s.key().equals(setting.key())) throw new IllegalArgumentException("Duplicate setting " + id + "." + setting.key());
        }
        setting.group = this;
        settings.add(setting);
        return setting;
    }

    public List<Setting<?, ?>> all() {
        return Collections.unmodifiableList(settings);
    }

    public Setting<?, ?> get(String key) {
        for (Setting<?, ?> s : settings) if (s.key().equals(key)) return s;
        return null;
    }

    /** Free-form JSON the owner may use for anything that is not a setting (caches, lists, ...). */
    public JsonObject data() {
        return data;
    }

    public Path file() {
        return configDir().resolve(id + ".json");
    }

    public static Path configDir() {
        return Vibey.platform().configDir().resolve(Vibey.MOD_ID);
    }

    /** Reads the file once (startup, client thread). Settings added later keep their defaults until set. */
    public void load() {
        loaded = true;
        Path f = file();
        if (!Files.isRegularFile(f)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Setting<?, ?> s : settings) {
                JsonElement v = json.get(s.key());
                if (v != null && !v.isJsonNull() && s.persisted()) s.load(v);
            }
            if (json.has("_data") && json.get("_data").isJsonObject()) data = json.getAsJsonObject("_data");
        } catch (Exception e) {
            Vibey.LOG.warn("Could not read {} ({}), using defaults", f, e.toString());
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** Schedules a save (snapshot now, write later on a worker thread). */
    public void markDirty() {
        Tasks.debounce(this, 400, this::saveNow);
    }

    /** Writes the file right now from the calling thread. */
    public void saveNow() {
        String text;
        synchronized (this) {
            JsonObject json = new JsonObject();
            for (Setting<?, ?> s : settings) if (s.persisted()) json.add(s.key(), s.toJson());
            if (data.size() > 0) json.add("_data", data.deepCopy());
            text = GSON.toJson(json);
        }
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, text, StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.writeString(f, text, StandardCharsets.UTF_8);
            } catch (IOException e2) {
                Vibey.LOG.warn("Could not save {}: {}", f, e2.toString());
            }
        }
    }
}
