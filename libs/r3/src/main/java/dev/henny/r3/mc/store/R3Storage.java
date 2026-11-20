package dev.henny.r3.mc.store;

import dev.henny.r3.hint.SetHintStore;
import dev.henny.r3.input.GestureBindings;
import dev.henny.r3.persist.ConfigStore;
import dev.henny.r3.persist.PlacementStore;
import dev.henny.r3.mc.compat.Game;

import java.nio.file.Path;

public final class R3Storage {
    private final ConfigStore files;
    private final SetHintStore hints;
    private String worldKey;
    private PlacementStore placements = new PlacementStore();

    public R3Storage(Path dir) {
        files = new ConfigStore(dir);
        hints = files.loadHints();
        hints.onChange(() -> files.saveHints(hints));
    }

    public static Path defaultDir() {
        return Game.mc().gameDirectory.toPath().resolve("config").resolve("r3");
    }

    public SetHintStore hints() {
        return hints;
    }

    public GestureBindings loadBindings(GestureBindings into) {
        return files.loadBindings(into);
    }

    public boolean saveBindings(GestureBindings bindings) {
        return files.saveBindings(bindings);
    }

    public String worldKey() {
        return worldKey;
    }

    public PlacementStore placements() {
        return placements;
    }

    public void openWorld(String key) {
        if (key.equals(worldKey)) return;
        saveWorld();
        worldKey = key;
        placements = files.loadWorld(key);
    }

    public void closeWorld() {
        saveWorld();
        worldKey = null;
        placements = new PlacementStore();
    }

    public boolean saveWorld() {
        return worldKey != null && files.saveWorld(worldKey, placements);
    }

    public ConfigStore files() {
        return files;
    }
}
