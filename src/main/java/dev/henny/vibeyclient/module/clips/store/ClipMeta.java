package dev.henny.vibeyclient.module.clips.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Builds {@code <id>.json}, schema 1 (docs/CLIPS.md §10.2). Fields that don't apply are left out, never null:
 * {@code server} only for multiplayer (and only when it may be saved), {@code world} only for singleplayer/hosting,
 * the auto-clip fields only for auto clips. Pure Java.
 */
public final class ClipMeta {
    private ClipMeta() {}

    public record Event(long tMs, String type, String victim) {}

    /** Everything known about a clip when it is saved. */
    public record Info(String id, Instant createdAt, long durationMs, long sizeBytes, int width, int height, int fps,
                       boolean audio, String reason, int kills, long highlightMs, List<Event> events,
                       String playerName, String playerUuid, String mode, String server, String world,
                       String minecraft, String loader, String clientVersion, String encoder, boolean thumbnail) {}

    public static JsonObject json(Info i) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", ClipStore.SCHEMA);
        o.addProperty("id", i.id());
        o.addProperty("created_at", i.createdAt().truncatedTo(ChronoUnit.SECONDS).toString());
        o.addProperty("video", i.id() + ".mp4");
        if (i.thumbnail()) o.addProperty("thumbnail", i.id() + ".jpg");
        o.addProperty("duration_ms", i.durationMs());
        o.addProperty("size_bytes", i.sizeBytes());
        o.addProperty("width", i.width());
        o.addProperty("height", i.height());
        o.addProperty("fps", i.fps());
        o.addProperty("video_codec", "h264");
        if (i.audio()) o.addProperty("audio_codec", "aac");
        o.addProperty("reason", i.reason());
        boolean auto = !"hotkey".equals(i.reason());
        if (auto && i.kills() > 0) o.addProperty("kills", i.kills());
        if (auto && i.highlightMs() >= 0) o.addProperty("highlight_ms", Math.min(i.highlightMs(), i.durationMs()));
        if (auto && i.events() != null && !i.events().isEmpty()) {
            JsonArray a = new JsonArray();
            for (Event e : i.events()) {
                if (a.size() >= 16) break;
                JsonObject ev = new JsonObject();
                ev.addProperty("t_ms", Math.max(0, Math.min(e.tMs(), i.durationMs())));
                ev.addProperty("type", e.type());
                if (e.victim() != null && !e.victim().isBlank()) ev.addProperty("victim", clip(e.victim(), 16));
                a.add(ev);
            }
            o.add("events", a);
        }
        if (i.playerName() != null) {
            JsonObject p = new JsonObject();
            p.addProperty("name", i.playerName());
            if (i.playerUuid() != null) p.addProperty("uuid", i.playerUuid());
            o.add("player", p);
        }
        if (i.mode() != null) o.addProperty("mode", i.mode());
        if ("multiplayer".equals(i.mode()) && i.server() != null && !i.server().isBlank()) o.addProperty("server", clip(i.server(), 255));
        if (("singleplayer".equals(i.mode()) || "hosting".equals(i.mode())) && i.world() != null && !i.world().isBlank()) {
            o.addProperty("world", clip(i.world(), 100));
        }
        if (i.minecraft() != null) o.addProperty("minecraft", i.minecraft());
        if (i.loader() != null) o.addProperty("loader", i.loader());
        o.addProperty("client", "vibey-client");
        if (i.clientVersion() != null) o.addProperty("client_version", i.clientVersion());
        if (i.encoder() != null) o.addProperty("encoder", i.encoder());
        o.addProperty("kept", false);
        return o;
    }

    private static String clip(String s, int max) {
        String t = s.strip();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
