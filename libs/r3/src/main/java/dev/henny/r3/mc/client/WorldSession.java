package dev.henny.r3.mc.client;

import dev.henny.r3.mc.R3Mod;
import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.mc.store.R3Storage;
import dev.henny.r3.mc.store.WorldKeyResolver;
import dev.henny.r3.scene.R3Scene;

import java.util.ArrayList;
import java.util.List;

final class WorldSession {
    private final R3Storage storage;
    private final R3Scene scene;
    private final List<WorldListener> listeners = new ArrayList<>();
    private Object level;
    private String key;

    WorldSession(R3Storage storage, R3Scene scene) {
        this.storage = storage;
        this.scene = scene;
    }

    void listen(WorldListener listener) {
        listeners.add(listener);
    }

    String key() {
        return key;
    }

    void check() {
        Object now = Game.level();
        if (now == level) return;
        if (level != null) left();
        level = now;
        if (now != null) joined();
    }

    private void joined() {
        key = WorldKeyResolver.current();
        storage.openWorld(key);
        R3Mod.LOG.debug("r3: world {}", key);
        for (WorldListener l : List.copyOf(listeners)) l.worldJoined(key);
    }

    private void left() {
        String old = key;
        scene.interaction().cancel();
        for (WorldListener l : List.copyOf(listeners)) l.worldLeft(old);
        scene.clear();
        storage.closeWorld();
        key = null;
    }
}
