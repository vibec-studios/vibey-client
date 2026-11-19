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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
    /** The file as read: values of settings added after {@link #load()} and keys this version doesn't know. */
    private JsonObject stored = new JsonObject();
    private boolean loaded;
    private final Map<String, Predicate<JsonObject>> migrations = new LinkedHashMap<>();

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
        // Added after the file was read (e.g. a media feature registered late): still gets its stored value.
        if (loaded && setting.persisted()) {
            JsonElement v = stored.get(setting.key());
            if (v != null && !v.isJsonNull()) setting.load(v);
        }
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

    /**
     * Registers a one-time change of the stored file (e.g. an old default that moved), before {@link #load()}. It
     * runs on the file's JSON before the values are read, once: the id is recorded in {@code _data.migrations}, also
     * for a file that did not need it, so a value the player sets again later is never touched.
     */
    public void migration(String id, Predicate<JsonObject> change) {
        migrations.put(id, change);
    }

    /** Runs the {@code migrations} not yet recorded in {@code json._data.migrations}; true if any ran. */
    public static boolean migrate(JsonObject json, Map<String, Predicate<JsonObject>> migrations) {
        if (migrations.isEmpty()) return false;
        JsonElement d = json.get("_data");
        JsonObject data = d != null && d.isJsonObject() ? d.getAsJsonObject() : null;
        boolean ran = false;
        for (Map.Entry<String, Predicate<JsonObject>> m : migrations.entrySet()) {
            if (data != null && dev.henny.vibeyclient.backend.DomainMove.done(data, m.getKey())) continue;
            boolean changed = m.getValue().test(json);
            if (data == null) {
                d = json.get("_data");
                data = d != null && d.isJsonObject() ? d.getAsJsonObject() : new JsonObject();
                json.add("_data", data);
            }
            dev.henny.vibeyclient.backend.DomainMove.mark(data, m.getKey());
            if (changed) Vibey.LOG.info("Settings migration {} changed stored values", m.getKey());
            ran = true;
        }
        return ran;
    }

    /** Reads the file once (startup, client thread). Settings added later get their stored value when added. */
    public void load() {
        loaded = true;
        Path f = file();
        if (!Files.isRegularFile(f)) {
            // nothing stored: nothing to migrate, but remember that the migrations are not needed anymore
            if (!migrations.isEmpty()) {
                JsonObject json = new JsonObject();
                migrate(json, migrations);
                data = json.getAsJsonObject("_data");
            }
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            boolean migrated = migrate(json, migrations);
            stored = json;
            for (Setting<?, ?> s : settings) {
                JsonElement v = json.get(s.key());
                if (v != null && !v.isJsonNull() && s.persisted()) s.load(v);
            }
            if (json.has("_data") && json.get("_data").isJsonObject()) data = json.getAsJsonObject("_data");
            if (migrated) markDirty();
        } catch (Exception e) {
            Vibey.LOG.warn("Could not read {} ({}), using defaults", f, e.toString());
        }
    }

    /** The file had a value for {@code key} when it was read (false for a setting new in this version). */
    public boolean stored(String key) {
        JsonElement v = stored.get(key);
        return v != null && !v.isJsonNull();
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
            // Unknown keys survive (a setting that is not declared in this run, e.g. of a part that is off).
            for (String k : stored.keySet()) if (!k.equals("_data")) json.add(k, stored.get(k));
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
