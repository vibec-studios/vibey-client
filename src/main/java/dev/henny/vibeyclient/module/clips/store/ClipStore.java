package dev.henny.vibeyclient.module.clips.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The clips folder {@code <gameDir>/vibeyclient/clips/} and its file contract (docs/CLIPS.md §10): {@code <id>.mp4},
 * {@code <id>.jpg}, {@code <id>.json}. A clip exists exactly when its JSON parses and its video exists. The JSON is
 * written last and atomically, edits keep unknown fields, deletion removes the JSON first. Pure Java (JUnit-tested);
 * blocking file I/O, never call it on the render thread.
 */
public final class ClipStore {
    public static final int SCHEMA = 1;
    public static final Pattern ID = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}_[a-z]+(_\\d+)?$");
    public static final int MAX_TITLE = 100, MAX_TAGS = 16, MAX_TAG = 32;
    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final long ORPHAN_AGE_MS = 3_600_000L;

    private final Path dir;
    /** Files a player or the launcher held open when we tried to delete them; retried on the next cleanup. */
    private final Set<Path> pendingDeletes = new LinkedHashSet<>();

    public ClipStore(Path dir) {
        this.dir = dir;
    }

    public Path dir() {
        return dir;
    }

    /** Hidden work folder for part files of a running save. */
    public Path tmpDir() {
        return dir.resolve(".tmp");
    }

    // ============================================================================================ reading

    /** One complete clip. {@code json} is the raw object (unknown fields included). */
    public record Clip(String id, JsonObject json, Instant createdAt, long durationMs, long sizeBytes, String reason,
                       boolean kept, String title, List<String> tags, Path video, Path thumbnail) {
        public String str(String key) {
            JsonElement e = json.get(key);
            return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
        }

        public int integer(String key, int def) {
            JsonElement e = json.get(key);
            try {
                return e != null && e.isJsonPrimitive() ? e.getAsInt() : def;
            } catch (NumberFormatException ex) {
                return def;
            }
        }

        public boolean auto() {
            return !"hotkey".equals(reason);
        }

        /** Size of the clip's files on disk right now (video + thumbnail + JSON). */
        public long filesBytes() {
            return size(video) + (thumbnail == null ? 0 : size(thumbnail)) + size(video.resolveSibling(id + ".json"));
        }
    }

    /** All complete clips, newest first. Unreadable JSONs are retried once after 200 ms, then skipped (never deleted). */
    public List<Clip> list() {
        List<Clip> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        List<Path> retry = new ArrayList<>();
        try (DirectoryStream<Path> s = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : s) {
                Clip c = readOrNull(p);
                if (c != null) out.add(c);
                else if (Files.exists(p) && ID.matcher(idOf(p)).matches()) retry.add(p);
            }
        } catch (IOException e) {
            return out;
        }
        if (!retry.isEmpty()) {
            sleep(200);
            for (Path p : retry) {
                Clip c = readOrNull(p);
                if (c != null) out.add(c);
            }
        }
        out.sort(Comparator.comparing(Clip::createdAt).reversed());
        return out;
    }

    public Clip get(String id) {
        if (!ID.matcher(id).matches()) return null;
        return readOrNull(dir.resolve(id + ".json"));
    }

    private static String idOf(Path json) {
        String n = json.getFileName().toString();
        return n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
    }

    private Clip readOrNull(Path json) {
        String id = idOf(json);
        if (!ID.matcher(id).matches()) return null;
        try {
            JsonObject o = JsonParser.parseString(Files.readString(json, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = o.has("schema") ? o.get("schema").getAsInt() : 0;
            if (schema < 1 || schema > SCHEMA) return null;
            String video = fileName(o, "video");
            if (video == null) return null;
            Path v = dir.resolve(video);
            if (!Files.isRegularFile(v)) return null;
            String thumb = fileName(o, "thumbnail");
            Path t = thumb == null ? null : dir.resolve(thumb);
            if (t != null && !Files.isRegularFile(t)) t = null;
            Instant created;
            try {
                created = Instant.parse(o.get("created_at").getAsString());
            } catch (RuntimeException e) {
                created = Files.getLastModifiedTime(json).toInstant();
            }
            List<String> tags = new ArrayList<>();
            if (o.get("tags") instanceof JsonArray a) for (JsonElement e : a) if (e.isJsonPrimitive()) tags.add(e.getAsString());
            return new Clip(id, o, created, longOr(o, "duration_ms", 0), longOr(o, "size_bytes", size(v)),
                    o.has("reason") ? o.get("reason").getAsString() : "clip", o.has("kept") && o.get("kept").getAsBoolean(),
                    o.has("title") && o.get("title").isJsonPrimitive() ? o.get("title").getAsString() : null, List.copyOf(tags), v, t);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** A plain file name in the same folder (no paths, no dotfiles), or null. */
    private static String fileName(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return null;
        String s = e.getAsString();
        if (s.isEmpty() || s.startsWith(".") || s.contains("/") || s.contains("\\") || s.contains(":")) return null;
        return s;
    }

    private static long longOr(JsonObject o, String key, long def) {
        try {
            return o.has(key) ? o.get(key).getAsLong() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    static long size(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    // ============================================================================================ writing

    /** A fresh id {@code YYYY-MM-DD_HH-mm-ss_<reason>} (with {@code _2}, {@code _3} … on a collision). */
    public String newId(LocalDateTime time, String reason) {
        String r = reason == null ? "clip" : reason.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (r.isEmpty()) r = "clip";
        String base = ID_TIME.format(time) + "_" + r;
        String id = base;
        for (int n = 2; taken(id); n++) id = base + "_" + n;
        return id;
    }

    private boolean taken(String id) {
        return Files.exists(dir.resolve(id + ".json")) || Files.exists(dir.resolve(id + ".mp4"))
                || Files.exists(dir.resolve("." + id + ".mp4.part")) || Files.exists(dir.resolve(id + ".jpg"));
    }

    /** Writes {@code <id>.json} via {@code .<id>.json.tmp} and an atomic rename (this marks the clip complete). */
    public void writeJson(String id, JsonObject json) throws IOException {
        Files.createDirectories(dir);
        Path target = dir.resolve(id + ".json");
        Path tmp = dir.resolve("." + id + ".json.tmp");
        Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
        move(tmp, target);
    }

    /** Renames a finished part file into place (atomic where the file system allows it). */
    public static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Windows: a reader (the launcher's watcher) may hold the target open for a moment
            sleep(60);
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Changes the user fields ({@code kept}, {@code title}, {@code tags}; null = unchanged, empty title = removed) and
     * keeps everything else. Returns the updated clip or null when it is gone.
     */
    public Clip edit(String id, Boolean kept, String title, Collection<String> tags) throws IOException {
        Path json = dir.resolve(id + ".json");
        if (!ID.matcher(id).matches() || !Files.isRegularFile(json)) return null;
        JsonObject o = JsonParser.parseString(Files.readString(json, StandardCharsets.UTF_8)).getAsJsonObject();
        if (kept != null) o.addProperty("kept", kept);
        if (title != null) {
            String t = title.strip();
            if (t.isEmpty()) o.remove("title");
            else o.addProperty("title", t.length() <= MAX_TITLE ? t : t.substring(0, MAX_TITLE));
        }
        if (tags != null) {
            JsonArray a = new JsonArray();
            Set<String> seen = new LinkedHashSet<>();
            for (String t : tags) {
                if (t == null) continue;
                String s = t.strip();
                if (s.isEmpty()) continue;
                if (s.length() > MAX_TAG) s = s.substring(0, MAX_TAG);
                if (seen.add(s) && seen.size() <= MAX_TAGS) a.add(s);
            }
            if (a.isEmpty()) o.remove("tags");
            else o.add("tags", a);
        }
        writeJson(id, o);
        return get(id);
    }

    /** Deletes a clip: the JSON first (it disappears for every reader at once), then video and thumbnail. */
    public boolean delete(String id) {
        if (!ID.matcher(id).matches()) return false;
        Clip c = get(id);
        Path json = dir.resolve(id + ".json");
        try {
            Files.deleteIfExists(json);
        } catch (IOException e) {
            return false;
        }
        List<Path> rest = new ArrayList<>();
        if (c != null) {
            rest.add(c.video());
            if (c.thumbnail() != null) rest.add(c.thumbnail());
        }
        rest.add(dir.resolve(id + ".mp4"));
        rest.add(dir.resolve(id + ".jpg"));
        for (Path p : rest) tryDelete(p);
        return true;
    }

    private void tryDelete(Path p) {
        try {
            Files.deleteIfExists(p);
            synchronized (pendingDeletes) {
                pendingDeletes.remove(p);
            }
        } catch (IOException e) {
            synchronized (pendingDeletes) {
                pendingDeletes.add(p); // locked by a player: later
            }
        }
    }

    // ============================================================================================ cleanup

    /** Result of {@link #cleanup}. */
    public record Cleanup(int deleted, long totalBytes, long keptBytes, boolean keptOverLimit) {}

    /**
     * While all clip files together are above {@code limitBytes}, deletes the oldest clip that is not kept. Never
     * deletes kept clips; {@code keptOverLimit} says when they alone are above the limit.
     */
    public Cleanup cleanup(long limitBytes) {
        retryPending();
        List<Clip> clips = new ArrayList<>(list());
        clips.sort(Comparator.comparing(Clip::createdAt)); // oldest first
        long total = 0, kept = 0;
        for (Clip c : clips) {
            long b = c.filesBytes();
            total += b;
            if (c.kept()) kept += b;
        }
        int deleted = 0;
        for (Clip c : clips) {
            if (total <= limitBytes) break;
            if (c.kept()) continue;
            long b = c.filesBytes();
            if (delete(c.id())) {
                total -= b;
                deleted++;
            }
        }
        return new Cleanup(deleted, total, kept, kept > limitBytes);
    }

    /** Retries deletes of files that were locked (e.g. by a player); true when nothing is pending any more. */
    public boolean retryPending() {
        List<Path> again;
        synchronized (pendingDeletes) {
            again = new ArrayList<>(pendingDeletes);
        }
        for (Path p : again) tryDelete(p);
        synchronized (pendingDeletes) {
            return pendingDeletes.isEmpty();
        }
    }

    /**
     * At start: removes a video/thumbnail without a JSON (a save that never finished), part/temp files older than an
     * hour and the work folder. Only names that follow the id pattern are touched.
     */
    public int removeOrphans(long nowMs) {
        if (!Files.isDirectory(dir)) return 0;
        int removed = 0;
        try (DirectoryStream<Path> s = Files.newDirectoryStream(dir)) {
            for (Path p : s) {
                String n = p.getFileName().toString();
                if (Files.isDirectory(p)) continue;
                if (n.startsWith(".") && (n.endsWith(".part") || n.endsWith(".tmp"))) {
                    String id = n.substring(1, n.indexOf('.', 1) > 0 ? n.indexOf('.', 1) : n.length());
                    if (ID.matcher(id).matches() && nowMs - mtime(p) > ORPHAN_AGE_MS && deleteQuiet(p)) removed++;
                    continue;
                }
                int dot = n.lastIndexOf('.');
                if (dot <= 0) continue;
                String id = n.substring(0, dot), ext = n.substring(dot + 1);
                if (!ext.equals("mp4") && !ext.equals("jpg")) continue;
                if (!ID.matcher(id).matches()) continue;
                if (!Files.exists(dir.resolve(id + ".json")) && nowMs - mtime(p) > 60_000L && deleteQuiet(p)) removed++;
            }
        } catch (IOException ignored) {
            // folder gone
        }
        Path tmp = tmpDir();
        if (Files.isDirectory(tmp)) {
            try (var w = Files.walk(tmp)) {
                for (Path p : w.sorted(Comparator.reverseOrder()).toList()) deleteQuiet(p);
            } catch (IOException ignored) {
                // in use
            }
        }
        return removed;
    }

    private static long mtime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE / 2;
        }
    }

    private static boolean deleteQuiet(Path p) {
        try {
            return Files.deleteIfExists(p);
        } catch (NoSuchFileException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** Total bytes of all complete clips (for the storage bar). */
    public long totalBytes() {
        long t = 0;
        for (Clip c : list()) t += c.filesBytes();
        return t;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
