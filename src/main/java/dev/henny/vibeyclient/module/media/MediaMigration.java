package dev.henny.vibeyclient.module.media;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.setting.SettingsGroup;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * The media module was called "lyrics" up to 0.2. On the first start with Media (no {@code media.json} yet) this
 * carries everything over, before any file is read:
 * <ul>
 *   <li>{@code config/vibeyclient/lyrics.json}: every setting {@code k} becomes {@code lyrics.k} in {@code media.json},
 *       {@code _data} becomes {@code _data.lyrics}; the old file is kept as {@code lyrics.json.migrated}</li>
 *   <li>{@code config/vibeyclient/modules.json}: the on/off state of "lyrics" becomes the one of "media"</li>
 *   <li>{@code options.txt}: keybinds {@code key.vibeyclient.lyrics.*} become {@code key.vibeyclient.media.lyrics.*}
 *       (the module is constructed before the game reads its options)</li>
 * </ul>
 * Runs on the client thread from the module constructor; failures are logged and never stop the game.
 */
final class MediaMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String OLD_KEY = "key_key.vibeyclient.lyrics.";
    private static final String NEW_KEY = "key_key.vibeyclient.media.lyrics.";

    private MediaMigration() {}

    static void run() {
        Path dir;
        try {
            dir = SettingsGroup.configDir();
        } catch (RuntimeException e) {
            return;
        }
        Path oldFile = dir.resolve("lyrics.json");
        Path newFile = dir.resolve(MediaModule.ID + ".json");
        if (!Files.isRegularFile(oldFile) || Files.exists(newFile)) return;
        try {
            JsonObject old = JsonParser.parseString(Files.readString(oldFile, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject out = new JsonObject();
            for (Map.Entry<String, JsonElement> e : old.entrySet()) {
                if (e.getKey().equals("_data")) {
                    JsonObject data = new JsonObject();
                    data.add("lyrics", e.getValue());
                    out.add("_data", data);
                } else {
                    out.add("lyrics." + e.getKey(), e.getValue());
                }
            }
            Files.createDirectories(dir);
            Files.writeString(newFile, GSON.toJson(out), StandardCharsets.UTF_8);
            Files.move(oldFile, dir.resolve("lyrics.json.migrated"), StandardCopyOption.REPLACE_EXISTING);
            Vibey.LOG.info("Moved the lyrics module settings to {}", newFile);
        } catch (Exception e) {
            Vibey.LOG.warn("Could not move {} to {}: {}", oldFile, newFile, e.toString());
        }
        moduleState(dir.resolve("modules.json"));
        keybinds(Vibey.platform().gameDir().resolve("options.txt"));
    }

    private static void moduleState(Path f) {
        if (!Files.isRegularFile(f)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("lyrics") || json.has(MediaModule.ID)) return;
            json.add(MediaModule.ID, json.remove("lyrics"));
            Files.writeString(f, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Vibey.LOG.warn("Could not update {}: {}", f, e.toString());
        }
    }

    private static void keybinds(Path f) {
        if (!Files.isRegularFile(f)) return;
        try {
            String text = Files.readString(f, StandardCharsets.UTF_8);
            if (!text.contains(OLD_KEY) || text.contains(NEW_KEY)) return;
            Files.writeString(f, text.replace(OLD_KEY, NEW_KEY), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Vibey.LOG.warn("Could not update the lyrics keybinds in {}: {}", f, e.toString());
        }
    }
}
