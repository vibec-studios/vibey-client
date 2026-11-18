package dev.henny.vibeyclient.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * {@code <instance>/.vibe/session.json}: what the game is doing right now, read by the Vibe Launcher for the Discord
 * status (contract: vibe-launcher {@code docs/PLAY-LINKS.md} §2). Pure Java: serialisation and the atomic write.
 */
public final class SessionFile {
    public static final String FILE = "session.json";
    public static final int SCHEMA = 1;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private SessionFile() {}

    public enum State {
        MENU, SINGLEPLAYER, HOSTING, MULTIPLAYER;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * One state of the game. Fields that do not apply are null: {@code server} only for multiplayer, {@code world}
     * only for singleplayer/hosting (the constructor enforces both), {@code players}/{@code maxPlayers} when known.
     */
    public record Snapshot(State state, String server, String world, String playLink, Integer players, Integer maxPlayers) {
        public Snapshot {
            if (state == null) state = State.MENU;
            if (state != State.MULTIPLAYER) server = null;
            if (state != State.SINGLEPLAYER && state != State.HOSTING) world = null;
            if (state != State.HOSTING) playLink = null;
            if (state == State.MENU) {
                players = null;
                maxPlayers = null;
            }
            server = blankToNull(server, 255);
            world = blankToNull(world, 100);
            playLink = blankToNull(playLink, 255);
        }

        public static Snapshot menu() {
            return new Snapshot(State.MENU, null, null, null, null, null);
        }
    }

    public static JsonObject toJson(Snapshot s, long updatedAtSeconds) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", SCHEMA);
        o.addProperty("state", s.state().id());
        if (s.server() != null) o.addProperty("server", s.server());
        if (s.world() != null) o.addProperty("world", s.world());
        if (s.playLink() != null) o.addProperty("play_link", s.playLink());
        if (s.players() != null) o.addProperty("players", s.players());
        if (s.maxPlayers() != null) o.addProperty("max_players", s.maxPlayers());
        o.addProperty("updated_at", updatedAtSeconds);
        return o;
    }

    public static String serialize(Snapshot s, long updatedAtSeconds) {
        return GSON.toJson(toJson(s, updatedAtSeconds));
    }

    /** Writes {@code <vibeDir>/session.json} atomically (temp file in the same folder, then move). Blocking I/O. */
    public static void write(Path vibeDir, Snapshot s, long updatedAtSeconds) throws IOException {
        Files.createDirectories(vibeDir);
        Path target = vibeDir.resolve(FILE);
        Path tmp = vibeDir.resolve(FILE + ".tmp");
        Files.writeString(tmp, serialize(s, updatedAtSeconds), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Windows: the launcher may hold the file open for a moment; one retry after a short pause.
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Removes the file (and a leftover temp file). Never throws. */
    public static void delete(Path vibeDir) {
        try {
            Files.deleteIfExists(vibeDir.resolve(FILE));
        } catch (IOException ignored) {
        }
        try {
            Files.deleteIfExists(vibeDir.resolve(FILE + ".tmp"));
        } catch (IOException ignored) {
        }
    }

    private static String blankToNull(String s, int max) {
        if (s == null) return null;
        String t = s.strip();
        if (t.isEmpty()) return null;
        return t.length() <= max ? t : t.substring(0, max);
    }
}
