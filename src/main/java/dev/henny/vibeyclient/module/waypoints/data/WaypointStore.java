package dev.henny.vibeyclient.module.waypoints.data;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointJson;
import dev.henny.vibeyclient.module.waypoints.core.WaypointList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;

public final class WaypointStore {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final WaypointList list = new WaypointList(WaypointStore::newId);
    private Path file;
    private int savedRevision;
    private boolean dirty;

    public WaypointList list() {
        return list;
    }

    public boolean loaded() {
        return file != null;
    }

    public void load(String worldKey) {
        file = StoreFiles.waypoints(worldKey);
        List<Waypoint> read = Files.isRegularFile(file) ? read(file) : List.of();
        list.replaceAll(read != null ? read : List.of());
        dirty = false;
    }

    private static List<Waypoint> read(Path file) {
        List<Waypoint> read = null;
        try {
            read = WaypointJson.parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            Vibey.LOG.warn("Waypoints: could not read {}: {}", file, e.toString());
        }
        if (read == null) StoreFiles.keepBroken(file);
        return read;
    }

    public void changed() {
        list.changed();
        save();
    }

    public void save() {
        if (file == null) return;
        dirty = true;
        Tasks.debounce(this, 600, () -> Tasks.onMain(this::writeNow));
    }

    public void unload() {
        if (dirty) writeNow();
        file = null;
        list.replaceAll(List.of());
    }

    private void writeNow() {
        if (file == null || !dirty) return;
        dirty = false;
        Path target = file;
        byte[] data = WaypointJson.write(list.all()).getBytes(StandardCharsets.UTF_8);
        Tasks.inOrder(() -> {
            try {
                StoreFiles.writeAtomically(target, data);
            } catch (IOException e) {
                Vibey.LOG.warn("Waypoints: could not save {}: {}", target, e.toString());
            }
        });
    }

    static String newId() {
        StringBuilder b = new StringBuilder(10);
        for (int i = 0; i < 10; i++) b.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return b.toString();
    }
}
